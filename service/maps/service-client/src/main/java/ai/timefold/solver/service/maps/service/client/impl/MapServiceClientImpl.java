package ai.timefold.solver.service.maps.service.client.impl;

import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_CACHE_ID;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_INVALIDATE_MATRIX_HEADER;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_LOCATIONS_CHUNK_BYTES;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_LOCATIONS_NOT_IN_MAP;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_MATRIX_HASH_HEADER;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_PROVIDER_HEADER;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_RESPONSE_CHUNK_BYTES;
import static ai.timefold.solver.service.definition.internal.Headers.X_TENANT_ID_HEADER;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.service.maps.haversine.impl.HaversineWaypointsProvider;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.impl.error.GoneRuntimeException;
import ai.timefold.solver.service.maps.service.integration.internal.MapServiceOptions;
import ai.timefold.solver.service.maps.service.integration.internal.model.IllegalDistanceResponseException;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceConverter;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.All;

public class MapServiceClientImpl implements MapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapServiceClientImpl.class);

    private final MapServiceClient mapService;
    private final List<TravelTimeAndDistanceConverter> converters;
    private final Optional<Boolean> fallbackEnabled;
    private final MapServiceLocalHaversineImpl fallbackService;
    private final SingleItemCache<CacheItem> travelTimeAndDistanceSingleItemCache;
    private final ObjectMapper mapper;

    @Inject
    public MapServiceClientImpl(@RestClient MapServiceClient mapService,
            @All List<TravelTimeAndDistanceConverter> converters,
            @ConfigProperty(name = "ai.timefold.platform.map-service.enable-fallback") Optional<Boolean> fallbackEnabled,
            HaversineTravelTimeAndDistanceMatrixProvider travelTimeAndDistanceMatrixProvider,
            HaversineWaypointsProvider haversineWaypointsProvider,
            ObjectMapper mapper) {
        this.mapService = mapService;
        this.converters = converters;
        this.fallbackEnabled = fallbackEnabled;
        this.mapper = mapper;
        fallbackService = new MapServiceLocalHaversineImpl(travelTimeAndDistanceMatrixProvider, haversineWaypointsProvider);
        travelTimeAndDistanceSingleItemCache = new SingleItemCache<>();
    }

    /**
     * Returns a matrices for the given locations.
     *
     * If the request contains a location set name and it's in local cache, check if there are updates since last stored hash.
     * If there are, update cache and return the updated matrices.
     * If there are no updates, return the matrices from cache.
     * If the location set does not exist in the maps-service, calculate again the matrices and store in cache by hash of
     * locations and options.
     *
     * If the request contains a location set name and it's not in cache, request location set from maps-service.
     * If location set exists, store in cache and return matrices.
     * If location set does not exist, request matrices from maps-service and store by hash of locations and options.
     *
     * If the request does not contain a location set name, calculate the distance matrix and store in cache by hash of
     * locations.
     *
     * @param locations the list of locations
     * @param options the options
     * @return the distance matrix with metadata
     */
    @Override
    public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
        Map<String, String> optionsMap = MapServiceOptions.parse(options);
        String locationSetName = optionsMap.getOrDefault(MapServiceOptions.LOCATION_SET_NAME, null);

        if (locations.size() < 2) {
            LOGGER.info("The number of locations is {}, generating empty distance matrix", locations.size());
            return generateZeroTravelTimeAndDistanceMatrixFromLocations(locations);
        }

        // If location set name is not empty and is in cache, fetch from cache and check if there are updates since last stored hash
        if (locationSetName != null && travelTimeAndDistanceSingleItemCache.isInCache(locationSetName)) {
            String matrixHash = travelTimeAndDistanceSingleItemCache.get().hash();
            Response response;
            try {
                LOGGER.info("Distance matrix with location set name {} in cache, fetching updates", locationSetName);
                response = mapService.getTravelTimeAndDistanceUpdates(options, matrixHash);
            } catch (GoneRuntimeException e) {
                // Location set was deleted from maps-service
                // Delete also from client cache, request again and store in local cache by hash of locations and options
                LOGGER.warn(
                        "Location set {} not in Maps Service but in client cache (probably deleted from the " +
                                "Maps Service). Will try to calculate entire distance matrix.",
                        locationSetName);
                travelTimeAndDistanceSingleItemCache.delete();
                return getFromCacheOrRequest(locations, options);
            }

            String invalidateMatrix = response.getHeaderString(X_MAPS_INVALIDATE_MATRIX_HEADER);
            List<Integer> chunkBytes = parseChunkBytesString(response.getHeaderString(X_MAPS_RESPONSE_CHUNK_BYTES));

            if (invalidateMatrix != null && invalidateMatrix.equals("true")) {
                // If distance matrix is invalid but there is new matrix for location set, store in cache by location set name
                LOGGER.info("Distance matrix in cache is invalid, processing new distance matrix from service");
                TravelTimeAndDistanceWithMetadata travelTimeAndDistance =
                        processResponseAndStoreInCache(response, locationSetName);
                assertLocationsAreInCache(locations);
                return travelTimeAndDistance;
            } else if (chunkBytes.isEmpty()) {
                // If there are no updates, return from cache
                LOGGER.info("Distance matrix in cache is up-to-date, returning from cache");
                assertLocationsAreInCache(locations);
                return new TravelTimeAndDistanceWithMetadata(travelTimeAndDistanceSingleItemCache.get().travelTimeAndDistance(),
                        travelTimeAndDistanceSingleItemCache.get().locationsOutOfMap());
            } else {
                // If there are updates, process them and update cache
                LOGGER.info("Distance matrix in cache is not up-to-date, processing updates");
                TravelTimeAndDistanceWithMetadata travelTimeAndDistance =
                        processUpdateAndStoreInCache(response, locationSetName);
                assertLocationsAreInCache(locations);
                return travelTimeAndDistance;
            }

        } else if (locationSetName != null) {
            LOGGER.info("Distance matrix with location set name {} not in cache, requesting it and storing it in cache",
                    locationSetName);

            // Check if location set exists but not in cache, request location set from maps-service
            String unknownMatrixHash = "00";
            Response response;
            try {
                response = mapService.getTravelTimeAndDistanceUpdates(options, unknownMatrixHash);
            } catch (GoneRuntimeException e) {
                // If location set does not exist, get from cache if id matches the hash of request, otherwise request
                return getFromCacheOrRequest(locations, options);
            }
            // If location set exists, store in cache by location set
            return processResponseAndStoreInCache(response, locationSetName);
        } else {
            // If location set name is empty, get from cache if id matches the hash of request, otherwise request
            return getFromCacheOrRequest(locations, options);
        }

    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        if (locations.size() < 2) {
            return locations;
        }
        try {
            return mapService.getWaypoints(locations, options);
        } catch (Exception e) {
            if (fallbackEnabled.orElse(false)) {
                LOGGER.warn("Could not get waypoints using maps service, will fallback using Haversine.", e);
                return fallbackService.getWaypoints(locations, options);
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        try {
            return mapService.getLocationsOutOfMap(locations, options);
        } catch (Exception e) {
            if (fallbackEnabled.orElse(false)) {
                LOGGER.warn("Could not get locations out of map using maps service, will fallback using Haversine.", e);
                return fallbackService.getLocationsOutOfMap(locations, options);
            } else {
                throw e;
            }
        }
    }

    private TravelTimeAndDistanceWithMetadata getFromCacheOrRequest(List<Location> locations, String options) {
        String id = String.valueOf(Objects.hash(new HashSet<>(locations), options));

        if (travelTimeAndDistanceSingleItemCache.isInCache(id)) {
            LOGGER.info("Distance matrix without location set name in cache, returning from cache");
            CacheItem cacheItem = travelTimeAndDistanceSingleItemCache.get();
            return new TravelTimeAndDistanceWithMetadata(cacheItem.travelTimeAndDistance(), cacheItem.locationsOutOfMap());
        }

        // If it does not exist, request from maps-service and store by hash of locations
        LOGGER.info("Distance matrix without location set name not in cache, requesting it and storing it in cache");
        return getAndStoreInCache(locations, options, id);
    }

    private TravelTimeAndDistanceWithMetadata getAndStoreInCache(List<Location> locations, String options,
            String localCacheId) {
        Response response;
        LOGGER.info("Requesting calculation of distance/time matrix for {} locations, this can take some time... ",
                locations.size());
        try {
            response = mapService.getTravelTimeAndDistance(locations, options);
        } catch (Exception e) {
            if (fallbackEnabled.orElse(false)) {
                LOGGER.warn("Could not get travel time and distance using maps service, will fallback using Haversine", e);
                return fallbackService.getTravelTimeAndDistance(locations, options);
            } else {
                throw e;
            }
        }

        TravelTimeAndDistanceWithMetadata travelTimeAndDistance = processResponseAndStoreInCache(response, localCacheId);
        LOGGER.info("Distance/time matrix calculation completed");
        return travelTimeAndDistance;
    }

    private TravelTimeAndDistanceWithMetadata processResponseAndStoreInCache(Response response, String localCacheId) {
        String matrixHash = response.getHeaderString(X_MAPS_MATRIX_HASH_HEADER);
        String provider = response.getHeaderString(X_MAPS_PROVIDER_HEADER);
        String tenant = response.getHeaderString(X_TENANT_ID_HEADER);
        String cacheId = response.getHeaderString(X_MAPS_CACHE_ID);
        String locationsNotInMapString = response.getHeaderString(X_MAPS_LOCATIONS_NOT_IN_MAP);
        List<Integer> chunkBytes = parseChunkBytesString(response.getHeaderString(X_MAPS_RESPONSE_CHUNK_BYTES));
        List<Integer> metadataBytes = parseChunkBytesString(response.getHeaderString(X_MAPS_LOCATIONS_CHUNK_BYTES));

        List<Integer> locationsNotInMap = new ArrayList<>();
        if (locationsNotInMapString != null && !locationsNotInMapString.isEmpty()) {
            locationsNotInMap = Arrays.stream(locationsNotInMapString.split(",")).map(Integer::valueOf).toList();
        }

        try {
            InputStream data = response.readEntity(InputStream.class);
            List<Location> responseLocations = readLocationsFromInputStream(data, metadataBytes);

            if (provider == null) {
                throw new IllegalArgumentException("No provider found to convert travel time and distance response.");
            }

            TravelTimeAndDistanceWithMetadata travelTimeAndDistance =
                    convertResponse(provider, chunkBytes, responseLocations, data, locationsNotInMap);
            travelTimeAndDistanceSingleItemCache.put(localCacheId,
                    new CacheItem(travelTimeAndDistance.travelTimeAndDistance(), responseLocations, matrixHash,
                            locationsNotInMap));
            return travelTimeAndDistance;

        } catch (IllegalDistanceResponseException e) {
            LOGGER.error("Travel time and distance using maps service was invalid", e);
            if (cacheId != null && !cacheId.isEmpty()) {
                mapService.cleanLocationSetsById(UUID.fromString(tenant), provider, cacheId);
            }
            throw e;
        }
    }

    private TravelTimeAndDistanceWithMetadata processUpdateAndStoreInCache(Response response, String locationSetName) {
        String matrixHash = response.getHeaderString(X_MAPS_MATRIX_HASH_HEADER);
        String provider = response.getHeaderString(X_MAPS_PROVIDER_HEADER);
        String tenant = response.getHeaderString(X_TENANT_ID_HEADER);
        String cacheId = response.getHeaderString(X_MAPS_CACHE_ID);
        String locationsNotInMapString = response.getHeaderString(X_MAPS_LOCATIONS_NOT_IN_MAP);
        List<Integer> chunkBytes = parseChunkBytesString(response.getHeaderString(X_MAPS_RESPONSE_CHUNK_BYTES));
        List<Integer> metadataBytes = parseChunkBytesString(response.getHeaderString(X_MAPS_LOCATIONS_CHUNK_BYTES));

        List<Integer> locationsNotInMap = new ArrayList<>();
        if (locationsNotInMapString != null && !locationsNotInMapString.isEmpty()) {
            locationsNotInMap = Arrays.stream(locationsNotInMapString.split(",")).map(Integer::valueOf).toList();
        }
        CacheItem cacheItem = travelTimeAndDistanceSingleItemCache.get();
        try {
            InputStream data = response.readEntity(InputStream.class);
            List<Location> responseLocations = readLocationsFromInputStream(data, metadataBytes);

            if (provider == null) {
                throw new IllegalArgumentException("No provider found to convert travel time and distance update.");
            }

            TravelTimeAndDistanceWithMetadata travelTimeAndDistance =
                    convertUpdate(provider, chunkBytes, responseLocations, data, cacheItem.locationsOutOfMap(),
                            locationsNotInMap);

            List<Location> newLocations = Stream.concat(cacheItem.locations().stream(), responseLocations.stream()).toList();
            if (locationSetName != null && matrixHash != null) {
                travelTimeAndDistanceSingleItemCache.put(locationSetName,
                        new CacheItem(travelTimeAndDistance.travelTimeAndDistance(), newLocations, matrixHash,
                                locationsNotInMap));
            }
            return travelTimeAndDistance;

        } catch (IllegalDistanceResponseException e) {
            LOGGER.error("Travel time and distance using maps service was invalid", e);
            if (cacheId != null && !cacheId.isEmpty()) {
                mapService.cleanLocationSetsById(UUID.fromString(tenant), provider, cacheId);
            }
            throw e;
        }
    }

    private TravelTimeAndDistanceWithMetadata convertResponse(String provider, List<Integer> chunkBytes,
            List<Location> locations, InputStream data, List<Integer> locationsNotInMap) {
        for (TravelTimeAndDistanceConverter converter : converters) {
            if (converter.canConvert(provider)) {
                if (chunkBytes.size() < 2) {
                    return converter.convert(locations, data, locationsNotInMap);
                } else {
                    return converter.convert(locations, data, chunkBytes, locationsNotInMap);
                }
            }
        }
        throw new IllegalArgumentException("No converter found for travel time and distance provider '" + provider + "'");
    }

    private TravelTimeAndDistanceWithMetadata convertUpdate(String provider, List<Integer> chunkBytes,
            List<Location> locations, InputStream data, List<Integer> oldLocationNotInMap, List<Integer> newLocationNotInMap) {
        for (TravelTimeAndDistanceConverter converter : converters) {
            if (converter.canConvert(provider)) {
                CacheItem cacheItem = travelTimeAndDistanceSingleItemCache.get();
                return converter.update(cacheItem.travelTimeAndDistance(), cacheItem.locations(), locations, data,
                        chunkBytes, oldLocationNotInMap, newLocationNotInMap);
            }
        }
        throw new IllegalArgumentException("No converter found for travel time and distance provider '" + provider + "'");
    }

    private List<Integer> parseChunkBytesString(String chunkBytesString) {
        if (chunkBytesString == null) {
            return new ArrayList<>();
        }
        String[] chunkBytesList = chunkBytesString.split(",");
        return Arrays.stream(chunkBytesList).map(Integer::valueOf).toList();
    }

    private TravelTimeAndDistanceWithMetadata generateZeroTravelTimeAndDistanceMatrixFromLocations(List<Location> locations) {
        DistanceMatrix matrix = DistanceMatrix.getInstance(locations.size());
        for (int i = 0; i < locations.size(); i++) {
            for (Location location : locations) {
                matrix.put(locations.get(i), location, 0);
            }
        }
        return new TravelTimeAndDistanceWithMetadata(new TravelTimeAndDistance(matrix, matrix), new ArrayList<>());
    }

    private List<Location> readLocationsFromInputStream(InputStream stream, List<Integer> chunkBytes) {
        List<Location> locations = new ArrayList<>();
        try {
            for (Integer chunkByte : chunkBytes) {
                if (chunkByte != null && chunkByte > 0) {
                    byte[] bytes = stream.readNBytes(chunkByte);
                    locations.addAll(mapper.readValue(bytes, new TypeReference<List<Location>>() {
                    }));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read locations from input stream", e);
        }
        return locations;
    }

    private void assertLocationsAreInCache(List<Location> locations) {
        List<Location> locationsInCache = travelTimeAndDistanceSingleItemCache.get().locations();
        Set<Location> locationsSet = new HashSet<>(locations);
        HashSet<Location> locationsInCacheSet = new HashSet<>(locationsInCache);
        if (!locationsSet.equals(locationsInCacheSet)) {
            throw new IllegalArgumentException("Locations received do not correspond to location set");
        }
    }
}
