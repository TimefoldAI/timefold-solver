package ai.timefold.solver.service.maps.service.client.api;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.enrichment.SolverModelEnricher;
import ai.timefold.solver.service.definition.internal.MapEnrichmentContext;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.TransportType;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByTimeframeWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceOptionsSupplier;
import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.service.maps.service.integration.api.LocationsAwareSolverModel;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceConverterException;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TravelTimeMatrixEnricher implements SolverModelEnricher<LocationsAwareSolverModel<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TravelTimeMatrixEnricher.class);

    private final MapService mapService;

    private final MapServiceOptionsSupplier optionsSupplier;

    private final boolean useTraffic;

    private final MapEnrichmentContext mapEnrichmentContext;

    @Inject
    public TravelTimeMatrixEnricher(MapService mapService, MapServiceOptionsSupplier optionsSupplier,
            MapEnrichmentContext mapEnrichmentContext,
            @ConfigProperty(name = "timefold.platform.map-service.use-traffic", defaultValue = "false") Boolean useTraffic) {
        this.mapService = mapService;
        this.optionsSupplier = optionsSupplier;
        this.mapEnrichmentContext = mapEnrichmentContext;
        this.useTraffic = useTraffic;
    }

    @Retry(maxRetries = 5, delay = 1, delayUnit = ChronoUnit.SECONDS, abortOn = {
            TravelTimeAndDistanceConverterException.class,
            IllegalArgumentException.class,
            MapServiceIllegalArgumentException.class
    })
    @Override
    public LocationsAwareSolverModel<?> enrich(LocationsAwareSolverModel<?> solverModel) {
        // One map-service round-trip per transport type; each mode resolves to its own OSRM instance. The first
        // mode is treated as primary and is the one whose map metadata (locations-not-in-map, resolved location) is
        // propagated to the solver model.
        List<TransportType> transportTypes = resolveTransportTypes(solverModel);
        for (var i = 0; i < transportTypes.size(); i++) {
            var transportType = transportTypes.get(i);
            boolean primary = i == 0;
            if (useTraffic) {
                enrichAllTimeframes(solverModel, transportType, primary);
            } else {
                enrichSingleMatrix(solverModel, transportType, primary);
            }
        }
        return solverModel;
    }

    private void enrichSingleMatrix(LocationsAwareSolverModel<?> solverModel, TransportType transportType,
            boolean primary) {
        List<Location> locations = solverModel.getLocations(); // Get all the locations from the model only once.
        TravelTimeAndDistanceWithMetadata travelTimeAndDistance;
        try {
            travelTimeAndDistance =
                    mapService.getTravelTimeAndDistance(locations,
                            optionsSupplier.getOptions(solverModel.getLocationSetName().orElse(null), transportType));
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Wrap in a non-recoverable error
            throw new TimefoldRuntimeException(ErrorCodes.MAP_SERVICE_UNKNOWN,
                    "Error getting travel time and distances from map service", e, false);
        }
        locations.forEach(location -> {
            location.setTravelTimeMatrix(transportType, travelTimeAndDistance.travelTimeAndDistance().travelTime());
            location.setDistanceMatrix(transportType, travelTimeAndDistance.travelTimeAndDistance().distance());
        });
        if (primary) {
            solverModel
                    .setLocationsNotInMap(convertIdxToLocations(travelTimeAndDistance.locationsNotInMapIdx(), locations));
            mapEnrichmentContext.setResolvedMapLocation(travelTimeAndDistance.resolvedMapLocation());
        }
    }

    private void enrichAllTimeframes(LocationsAwareSolverModel<?> solverModel, TransportType transportType,
            boolean primary) {
        List<Location> locations = solverModel.getLocations();
        TravelTimesByTimeframeWithMetadata result;
        try {
            result = mapService.getTravelTimeAndDistanceByTimeframe(locations, optionsSupplier.getOptions(transportType));
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new TimefoldRuntimeException(ErrorCodes.MAP_SERVICE_UNKNOWN,
                    "Error getting travel time and distances from map service", e, false);
        }

        DistanceMatrix[] travelTimes = result.travelTimesByTimeframe();
        DistanceMatrix[] distances = result.distancesByTimeframe();
        if (travelTimes.length == 1) {
            // Single timeframe (e.g. a single-timeframe bucketing): stamp the scalar matrices so lookups use the
            // IndexableDistanceMatrix index-cache fast path. The time-aware overloads keep working because Location
            // falls back to the single matrix when no per-timeframe matrices are set.
            for (Location location : locations) {
                location.setTravelTimeMatrix(transportType, travelTimes[0]);
                location.setDistanceMatrix(transportType, distances[0]);
            }
        } else {
            for (Location location : locations) {
                location.setTravelTimeMatrices(transportType, travelTimes, result.timeframeIndexResolver());
                location.setDistanceMatrices(transportType, distances, result.timeframeIndexResolver());
            }
        }
        if (primary) {
            solverModel.setLocationsNotInMap(result.locationsNotInMap());
        }
    }

    @Override
    public boolean accept(Object solverModel) {
        return solverModel instanceof LocationsAwareSolverModel;
    }

    private List<Location> convertIdxToLocations(List<Integer> idx, List<Location> locations) {
        if (idx == null) {
            return Collections.emptyList();
        }
        List<Location> locationList = idx.stream().filter(i -> i != null && i < locations.size()).map(locations::get).toList();

        if (locationList.size() != idx.size()) {
            LOGGER.warn("Some locations out of map for the solution were not found in the provided locations.");
        }

        return locationList;
    }

    /**
     * Decides which transport types travel times are fetched for, and rejects the dataset when it uses a transport
     * type this deployment is not allowed to route with.
     */
    private List<TransportType> resolveTransportTypes(LocationsAwareSolverModel<?> solverModel) {
        List<TransportType> datasetTransportTypes = solverModel.getTransportTypes().stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted() // Deterministic order; CAR, when used, comes first and is therefore the primary one.
                .toList();
        if (!optionsSupplier.isAutoSelectTransportType()) {
            TransportType configuredTransportType = optionsSupplier.getTransportType();
            failIfNotAllowed(List.of(configuredTransportType));
            List<TransportType> unsupported = datasetTransportTypes.stream()
                    .filter(transportType -> transportType != configuredTransportType)
                    .toList();
            if (!unsupported.isEmpty()) {
                throw new MapServiceIllegalArgumentException(ErrorCodes.MAP_SERVICE_TRANSPORT_TYPE_NOT_ALLOWED,
                        ("The dataset uses transport type(s) (%s) other than the configured transport type (%s). "
                                + "Configure the transport type (%s) to route a single dataset with several transport types.")
                                .formatted(join(unsupported), configuredTransportType, TransportType.AUTO_SELECT),
                        false);
            }
            return List.of(configuredTransportType);
        }
        if (datasetTransportTypes.isEmpty()) {
            return List.of(optionsSupplier.getDefaultTransportType());
        }
        failIfNotAllowed(datasetTransportTypes);
        return datasetTransportTypes;
    }

    private void failIfNotAllowed(List<TransportType> transportTypes) {
        List<TransportType> notAllowed = transportTypes.stream()
                .filter(transportType -> !optionsSupplier.isAllowed(transportType))
                .toList();
        if (notAllowed.isEmpty()) {
            return;
        }
        List<TransportType> allowed = optionsSupplier.getAllowedTransportTypes();
        throw new MapServiceIllegalArgumentException(ErrorCodes.MAP_SERVICE_TRANSPORT_TYPE_NOT_ALLOWED,
                "The transport type(s) (%s) are not allowed; the allowed transport types are (%s)."
                        .formatted(join(notAllowed), join(allowed.isEmpty() ? TransportType.ROUTING_PROFILES : allowed)),
                false);
    }

    private static String join(List<TransportType> transportTypes) {
        return transportTypes.stream().map(TransportType::value).collect(Collectors.joining(", "));
    }
}
