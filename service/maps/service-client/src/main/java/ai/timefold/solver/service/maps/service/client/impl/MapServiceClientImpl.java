package ai.timefold.solver.service.maps.service.client.impl;

import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_CACHE_ID;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_INVALIDATE_MATRIX_HEADER;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_LOCATIONS_CHUNK_BYTES;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_LOCATIONS_NOT_IN_MAP;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_LOCATION_HEADER;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_MATRIX_HASH_HEADER;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_PROVIDER_HEADER;
import static ai.timefold.solver.service.definition.internal.Headers.X_MAPS_RESPONSE_CHUNK_BYTES;
import static ai.timefold.solver.service.definition.internal.Headers.X_TENANT_ID_HEADER;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.TimeInterval;
import ai.timefold.solver.service.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.service.maps.haversine.impl.HaversineWaypointsProvider;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByAvailabilityWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.Timeframe;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.client.impl.error.GoneRuntimeException;
import ai.timefold.solver.service.maps.service.integration.internal.MapServiceOptions;
import ai.timefold.solver.service.maps.service.integration.internal.model.IllegalDistanceResponseException;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceConverter;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
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
    private final boolean useTraffic;
    private final Timeframe defaultTimeframe;
    private final MapServiceLocalHaversineImpl fallbackService;
    private final SingleItemCache<CacheItem> travelTimeAndDistanceSingleItemCache;
    private final SingleItemCache<TravelTimesByAvailabilityWithMetadata> availabilityCache;
    private final TimeframeBucketing timeframeBucketing;
    private final ManagedExecutor managedExecutor;
    private final ObjectMapper mapper;

    @Inject
    public MapServiceClientImpl(@RestClient MapServiceClient mapService,
            @All List<TravelTimeAndDistanceConverter> converters,
            @ConfigProperty(name = "ai.timefold.platform.map-service.enable-fallback") Optional<Boolean> fallbackEnabled,
            @ConfigProperty(name = "ai.timefold.platform.map-service.use-traffic") Optional<Boolean> useTraffic,
            @ConfigProperty(
                    name = "ai.timefold.platform.map-service.default-timeframe") Optional<String> defaultTimeframeOverride,
            HaversineTravelTimeAndDistanceMatrixProvider travelTimeAndDistanceMatrixProvider,
            HaversineWaypointsProvider haversineWaypointsProvider,
            TimeframeBucketing timeframeBucketing,
            ManagedExecutor managedExecutor,
            ObjectMapper mapper) {
        this.mapService = mapService;
        this.converters = converters;
        this.fallbackEnabled = fallbackEnabled;
        this.useTraffic = useTraffic.orElse(false);
        this.timeframeBucketing = timeframeBucketing;
        this.defaultTimeframe = resolveDefaultTimeframe(timeframeBucketing, defaultTimeframeOverride);
        this.managedExecutor = managedExecutor;
        this.mapper = mapper;
        fallbackService = new MapServiceLocalHaversineImpl(travelTimeAndDistanceMatrixProvider, haversineWaypointsProvider);
        travelTimeAndDistanceSingleItemCache = new SingleItemCache<>();
        availabilityCache = new SingleItemCache<>();
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

        // Traffic-without-pruning path: when traffic is enabled platform-wide but the caller didn't pick a timeframe,
        // append the bucketing's default so the maps service returns a single traffic-aware matrix instead of plain
        // (timeframe-independent) data. The model keeps using location.getTravelTimeTo(other).
        if (useTraffic && !optionsMap.containsKey(MapServiceOptions.TIMEFRAME)) {
            options = MapServiceOptions.withOption(options, MapServiceOptions.TIMEFRAME, defaultTimeframe.name());
        }

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
                CacheItem cached = travelTimeAndDistanceSingleItemCache.get();
                return new TravelTimeAndDistanceWithMetadata(cached.travelTimeAndDistance(),
                        cached.locationsOutOfMap(), cached.resolvedMapLocation());
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
    public TravelTimesByAvailabilityWithMetadata getTravelTimeAndDistance(List<Location> locations, String options,
            Map<Location, List<TimeInterval>> timeAvailability) {
        // Build per-timeframe location subsets: a location is only included in timeframes it's actually available in,
        // so the maps service doesn't compute cells we won't use.
        Map<Timeframe, LinkedHashSet<Location>> locationsByTimeframe = new LinkedHashMap<>();
        for (Map.Entry<Location, List<TimeInterval>> entry : timeAvailability.entrySet()) {
            for (TimeInterval interval : entry.getValue()) {
                for (Timeframe timeframe : timeframeBucketing.timeframesOf(interval.from(), interval.to())) {
                    locationsByTimeframe.computeIfAbsent(timeframe, k -> new LinkedHashSet<>()).add(entry.getKey());
                }
            }
        }
        if (locationsByTimeframe.isEmpty()) {
            throw new IllegalArgumentException(
                    "timeAvailability contained no intervals; at least one is required to derive a timeframe.");
        }

        String cacheId = String.valueOf(Objects.hash(new HashSet<>(locations), options, timeAvailability));
        if (availabilityCache.isInCache(cacheId)) {
            LOGGER.info("Availability matrices in cache, returning from cache");
            return availabilityCache.get();
        }

        if (!useTraffic) {
            LOGGER.info("Traffic disabled by platform configuration; using non-traffic matrix for availability request");
            TravelTimeAndDistanceWithMetadata plain = getTravelTimeAndDistance(locations, options);
            Set<Location> notInMapSet = new HashSet<>();
            for (Integer idx : plain.locationsNotInMapIdx()) {
                if (idx != null && idx >= 0 && idx < locations.size()) {
                    notInMapSet.add(locations.get(idx));
                }
            }
            List<Location> locationsNotInMap = locations.stream().filter(notInMapSet::contains).toList();
            TravelTimesByAvailabilityWithMetadata result = new TravelTimesByAvailabilityWithMetadata(
                    new DistanceMatrix[] { plain.travelTimeAndDistance().travelTime() },
                    new DistanceMatrix[] { plain.travelTimeAndDistance().distance() },
                    locationsNotInMap,
                    t -> 0);
            availabilityCache.put(cacheId, result);
            return result;
        }

        AssembledTimeframedMatrices assembled;
        try {
            assembled = fetchBundledTimeframedMatrices(options, locationsByTimeframe, locations);
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (fallbackEnabled.orElse(false)) {
                LOGGER.warn("Could not get travel time and distance using maps service, will fallback using Haversine.", cause);
                return fallbackAvailabilityMatrices(locations, options, locationsByTimeframe);
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(cause);
        }

        TravelTimesByAvailabilityWithMetadata result = new TravelTimesByAvailabilityWithMetadata(
                assembled.travelTimesByTimeframe,
                assembled.distancesByTimeframe,
                assembled.locationsNotInMap,
                timeframeBucketing::indexOf);
        availabilityCache.put(cacheId, result);
        LOGGER.info("Distance/time matrix calculation completed");
        return result;
    }

    /**
     * Fans out one bundled-response request per timeframe (no {@code annotation} option, so the server returns both
     * travel-time and distance matrices). Each call sends only the locations available in its timeframe. A timeframe
     * whose subset has fewer than two locations is short-circuited to a locally-generated zero matrix without a server
     * round-trip.
     * <p>
     * The produced {@code travelTimesByTimeframe} and {@code distancesByTimeframe} arrays are sized to the full
     * {@code TimeframeBucketing.allTimeframes().size()}; cells for timeframes that aren't needed by the model's
     * availability are left {@code null}. Solver-side lookups on those cells will throw through
     * {@link Location#getTravelTimeTo(Location, OffsetDateTime)}.
     * <p>
     * "Locations not in map" indices returned by each call are relative to that call's subset; the assembly step
     * resolves them to {@link Location} instances, then filters {@code outerLocations} against that set to produce a
     * final list in outer-list order with no duplicates.
     */
    private AssembledTimeframedMatrices fetchBundledTimeframedMatrices(String options,
            Map<Timeframe, LinkedHashSet<Location>> locationsByTimeframe,
            List<Location> outerLocations) {
        List<Timeframe> allTimeframes = timeframeBucketing.allTimeframes();
        int n = allTimeframes.size();
        LOGGER.info("Requesting up to {} bundled travel-time + distance matrix(es) concurrently... ",
                locationsByTimeframe.size());

        DistanceMatrix[] travelTimesByTimeframe = new DistanceMatrix[n];
        DistanceMatrix[] distancesByTimeframe = new DistanceMatrix[n];
        Set<Location> notInMapSet = new HashSet<>();

        // Futures indexed by bucketing position; null means either "timeframe not needed" or "zero-matrix short-circuit".
        CompletableFuture<TravelTimeAndDistanceWithMetadata>[] futures = new CompletableFuture[n];
        List<Location>[] subsets = new List[n];

        for (int idx = 0; idx < n; idx++) {
            Timeframe timeframe = allTimeframes.get(idx);
            LinkedHashSet<Location> subsetRaw = locationsByTimeframe.get(timeframe);
            if (subsetRaw == null) {
                continue; // timeframe not needed by the model — leave array cells null
            }
            List<Location> subset = new ArrayList<>(subsetRaw);
            subsets[idx] = subset;
            if (subset.size() < 2) {
                // Not enough locations to need a matrix — short-circuit to a zero matrix, skip the server call.
                DistanceMatrix zero = zeroMatrixFor(subset);
                travelTimesByTimeframe[idx] = zero;
                distancesByTimeframe[idx] = zero;
                continue;
            }
            String opts = MapServiceOptions.withOption(options, MapServiceOptions.TIMEFRAME, timeframe.name());
            futures[idx] = CompletableFuture.supplyAsync(() -> requestAndConvert(subset, opts), managedExecutor);
        }

        CompletableFuture<?>[] pending = Arrays.stream(futures).filter(Objects::nonNull)
                .toArray(CompletableFuture<?>[]::new);
        if (pending.length > 0) {
            CompletableFuture.allOf(pending).join();
        }

        for (int idx = 0; idx < n; idx++) {
            CompletableFuture<TravelTimeAndDistanceWithMetadata> future = futures[idx];
            if (future == null) {
                continue; // already filled (zero matrix) or not needed
            }
            TravelTimeAndDistanceWithMetadata response = future.resultNow();
            travelTimesByTimeframe[idx] = response.travelTimeAndDistance().travelTime();
            distancesByTimeframe[idx] = response.travelTimeAndDistance().distance();
            List<Location> subset = subsets[idx];
            for (int subsetIdx : response.locationsNotInMapIdx()) {
                if (subsetIdx >= 0 && subsetIdx < subset.size()) {
                    notInMapSet.add(subset.get(subsetIdx));
                }
            }
        }

        // One pass over the canonical outer list gives outer-list order and dedup at the same time.
        List<Location> locationsNotInMap = outerLocations.stream()
                .filter(notInMapSet::contains)
                .toList();
        return new AssembledTimeframedMatrices(travelTimesByTimeframe, distancesByTimeframe, locationsNotInMap);
    }

    private record AssembledTimeframedMatrices(DistanceMatrix[] travelTimesByTimeframe,
            DistanceMatrix[] distancesByTimeframe,
            List<Location> locationsNotInMap) {
    }

    private DistanceMatrix zeroMatrixFor(List<Location> locations) {
        DistanceMatrix matrix = DistanceMatrix.getInstance(locations.size());
        for (Location from : locations) {
            for (Location to : locations) {
                matrix.put(from, to, 0);
            }
        }
        return matrix;
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
            return new TravelTimeAndDistanceWithMetadata(cacheItem.travelTimeAndDistance(), cacheItem.locationsOutOfMap(),
                    cacheItem.resolvedMapLocation());
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
        String resolvedMapLocation = response.getHeaderString(X_MAPS_LOCATION_HEADER);
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

            TravelTimeAndDistanceWithMetadata raw =
                    convertResponse(provider, chunkBytes, responseLocations, data, locationsNotInMap);
            TravelTimeAndDistanceWithMetadata travelTimeAndDistance = new TravelTimeAndDistanceWithMetadata(
                    raw.travelTimeAndDistance(), raw.locationsNotInMapIdx(), resolvedMapLocation);
            travelTimeAndDistanceSingleItemCache.put(localCacheId,
                    new CacheItem(travelTimeAndDistance.travelTimeAndDistance(), responseLocations, matrixHash,
                            locationsNotInMap, resolvedMapLocation));
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
        String resolvedMapLocation = response.getHeaderString(X_MAPS_LOCATION_HEADER);
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

            TravelTimeAndDistanceWithMetadata raw =
                    convertUpdate(provider, chunkBytes, responseLocations, data, cacheItem.locationsOutOfMap(),
                            locationsNotInMap);
            String effectiveMapLocation = resolvedMapLocation != null ? resolvedMapLocation : cacheItem.resolvedMapLocation();
            TravelTimeAndDistanceWithMetadata travelTimeAndDistance = new TravelTimeAndDistanceWithMetadata(
                    raw.travelTimeAndDistance(), raw.locationsNotInMapIdx(), effectiveMapLocation);

            List<Location> newLocations = Stream.concat(cacheItem.locations().stream(), responseLocations.stream()).toList();
            if (locationSetName != null && matrixHash != null) {
                travelTimeAndDistanceSingleItemCache.put(locationSetName,
                        new CacheItem(travelTimeAndDistance.travelTimeAndDistance(), newLocations, matrixHash,
                                locationsNotInMap, effectiveMapLocation));
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

    private TravelTimeAndDistanceWithMetadata requestAndConvert(List<Location> locations, String options) {
        Response response = mapService.getTravelTimeAndDistance(locations, options);
        String provider = response.getHeaderString(X_MAPS_PROVIDER_HEADER);
        String locationsNotInMapString = response.getHeaderString(X_MAPS_LOCATIONS_NOT_IN_MAP);
        List<Integer> chunkBytes = parseChunkBytesString(response.getHeaderString(X_MAPS_RESPONSE_CHUNK_BYTES));
        List<Integer> metadataBytes = parseChunkBytesString(response.getHeaderString(X_MAPS_LOCATIONS_CHUNK_BYTES));

        List<Integer> locationsNotInMap = new ArrayList<>();
        if (locationsNotInMapString != null && !locationsNotInMapString.isEmpty()) {
            locationsNotInMap = Arrays.stream(locationsNotInMapString.split(",")).map(Integer::valueOf).toList();
        }

        InputStream data = response.readEntity(InputStream.class);
        List<Location> responseLocations = readLocationsFromInputStream(data, metadataBytes);

        if (provider == null) {
            throw new IllegalArgumentException("No provider found to convert travel time and distance response.");
        }

        return convertResponse(provider, chunkBytes, responseLocations, data, locationsNotInMap);
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

    private TravelTimesByAvailabilityWithMetadata fallbackAvailabilityMatrices(List<Location> locations, String options,
            Map<Timeframe, LinkedHashSet<Location>> locationsByTimeframe) {
        TravelTimeAndDistanceWithMetadata fallback = fallbackService.getTravelTimeAndDistance(locations, options);
        List<Timeframe> allTimeframes = timeframeBucketing.allTimeframes();
        DistanceMatrix[] travelTimesByTimeframe = new DistanceMatrix[allTimeframes.size()];
        DistanceMatrix[] distancesByTimeframe = new DistanceMatrix[allTimeframes.size()];
        DistanceMatrix fallbackTravelTime = fallback.travelTimeAndDistance().travelTime();
        DistanceMatrix fallbackDistance = fallback.travelTimeAndDistance().distance();
        for (int idx = 0; idx < allTimeframes.size(); idx++) {
            if (locationsByTimeframe.containsKey(allTimeframes.get(idx))) {
                travelTimesByTimeframe[idx] = fallbackTravelTime;
                distancesByTimeframe[idx] = fallbackDistance;
            }
        }
        Set<Location> notInMapSet = new HashSet<>();
        for (int idx : fallback.locationsNotInMapIdx()) {
            if (idx >= 0 && idx < locations.size()) {
                notInMapSet.add(locations.get(idx));
            }
        }
        List<Location> locationsNotInMap = locations.stream().filter(notInMapSet::contains).toList();
        return new TravelTimesByAvailabilityWithMetadata(travelTimesByTimeframe, distancesByTimeframe,
                locationsNotInMap, timeframeBucketing::indexOf);
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

    private Timeframe resolveDefaultTimeframe(TimeframeBucketing bucketing, Optional<String> override) {
        if (override.isEmpty() || override.get().isBlank()) {
            return bucketing.defaultTimeframe();
        }
        String requested = override.get();
        return bucketing.allTimeframes().stream()
                .filter(t -> t.name().equalsIgnoreCase(requested))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        ("Configured default timeframe '%s' is not one of the supported timeframes %s. " +
                                "Check the ai.timefold.platform.map-service.default-timeframe property.")
                                .formatted(requested, bucketing.allTimeframes())));
    }
}
