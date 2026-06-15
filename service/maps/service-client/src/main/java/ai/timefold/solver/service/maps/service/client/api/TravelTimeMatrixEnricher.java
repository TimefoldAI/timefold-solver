package ai.timefold.solver.service.maps.service.client.api;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.enrichment.SolverModelEnricher;
import ai.timefold.solver.service.definition.internal.MapEnrichmentContext;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.TimeInterval;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByAvailabilityWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceOptionsSupplier;
import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.service.maps.service.integration.api.LocationsAwareSolverModel;
import ai.timefold.solver.service.maps.service.integration.api.TimeAwareLocationsSolverModel;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceConverterException;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TravelTimeMatrixEnricher implements SolverModelEnricher<LocationsAwareSolverModel<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TravelTimeMatrixEnricher.class);

    private final MapService mapService;

    private final MapServiceOptionsSupplier optionsSupplier;

    private final MapEnrichmentContext mapEnrichmentContext;

    @Inject
    public TravelTimeMatrixEnricher(MapService mapService, MapServiceOptionsSupplier optionsSupplier,
            MapEnrichmentContext mapEnrichmentContext) {
        this.mapService = mapService;
        this.optionsSupplier = optionsSupplier;
        this.mapEnrichmentContext = mapEnrichmentContext;
    }

    @Retry(maxRetries = 5, delay = 1, delayUnit = ChronoUnit.SECONDS, abortOn = {
            TravelTimeAndDistanceConverterException.class,
            IllegalArgumentException.class,
            MapServiceIllegalArgumentException.class
    })
    @Override
    public LocationsAwareSolverModel<?> enrich(LocationsAwareSolverModel<?> solverModel) {
        // Two enricher paths driven by the model interface:
        //   - LocationsAwareSolverModel              -> single matrix, scalar setters. Whether the underlying data is
        //     plain or traffic-aware-at-default-timeframe is decided inside MapServiceClientImpl based on the
        //     use-traffic flag.
        //   - TimeAwareLocationsSolverModel          -> per-timeframe matrices, array setters. With traffic on, that's
        //     one matrix per overlapping timeframe; with traffic off, MapServiceClientImpl wraps a single plain matrix
        //     as a one-bucket array (resolver always returns 0).
        // The use-traffic config flag lives in MapServiceClientImpl; the enricher doesn't read it.
        if (solverModel instanceof TimeAwareLocationsSolverModel<?> timeAwareSolverModel) {
            return enrichAvailabilityAware(timeAwareSolverModel);
        }
        return enrichSingleMatrix(solverModel);
    }

    private LocationsAwareSolverModel<?> enrichSingleMatrix(LocationsAwareSolverModel<?> solverModel) {
        List<Location> locations = solverModel.getLocations(); // Get all the locations from the model only once.
        TravelTimeAndDistanceWithMetadata travelTimeAndDistance;
        try {
            travelTimeAndDistance =
                    mapService.getTravelTimeAndDistance(locations,
                            optionsSupplier.getOptions(solverModel.getLocationSetName()));
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Wrap in a non-recoverable error
            throw new TimefoldRuntimeException(ErrorCodes.MAP_SERVICE_UNKNOWN,
                    "Error getting travel time and distances from map service", e, false);
        }
        locations.forEach(location -> {
            location.setTravelTimeMatrix(travelTimeAndDistance.travelTimeAndDistance().travelTime());
            location.setDistanceMatrix(travelTimeAndDistance.travelTimeAndDistance().distance());
        });
        solverModel.setLocationsNotInMap(convertIdxToLocations(travelTimeAndDistance.locationsNotInMapIdx(), locations));
        mapEnrichmentContext.setResolvedMapLocation(travelTimeAndDistance.resolvedMapLocation());
        return solverModel;
    }

    private LocationsAwareSolverModel<?> enrichAvailabilityAware(TimeAwareLocationsSolverModel<?> solverModel) {
        List<Location> locations = solverModel.getLocations();
        Map<Location, List<TimeInterval>> availability = solverModel.getLocationsWithTimeAvailability();

        TravelTimesByAvailabilityWithMetadata result;
        try {
            result = mapService.getTravelTimeAndDistance(locations, optionsSupplier.getOptions(), availability);
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new TimefoldRuntimeException(ErrorCodes.MAP_SERVICE_UNKNOWN,
                    "Error getting travel time and distances from map service", e, false);
        }

        for (Location location : locations) {
            location.setTravelTimeMatrices(result.travelTimesByTimeframe(), result.timeframeIndexResolver());
            location.setDistanceMatrices(result.distancesByTimeframe(), result.timeframeIndexResolver());
        }
        solverModel.setLocationsNotInMap(result.locationsNotInMap());
        return solverModel;
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
}
