package ai.timefold.solver.model.maps.service.client.api;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.api.enrichment.SolverModelEnricher;
import ai.timefold.solver.model.definition.internal.MapEnrichmentContext;
import ai.timefold.solver.model.definition.internal.error.ErrorCodes;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.service.client.impl.MapServiceOptionsSupplier;
import ai.timefold.solver.model.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.model.maps.service.integration.api.LocationsAwareSolverModel;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistanceConverterException;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

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
