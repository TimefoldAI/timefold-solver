package ai.timefold.solver.service.maps.service.client.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.enrichment.SolverModelEnricher;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.TimeInterval;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByAvailabilityWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.MapServiceOptionsSupplier;
import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.service.maps.service.integration.api.LocationsAwareSolverModel;
import ai.timefold.solver.service.maps.service.integration.api.TimeAwareLocationsSolverModel;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceConverterException;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TravelTimeMatrixEnricher implements SolverModelEnricher<LocationsAwareSolverModel<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TravelTimeMatrixEnricher.class);

    private static final TimeInterval FULL_DAY = new TimeInterval(
            OffsetDateTime.of(LocalDate.EPOCH, LocalTime.MIDNIGHT, ZoneOffset.UTC),
            OffsetDateTime.of(LocalDate.EPOCH.plusDays(1), LocalTime.MIDNIGHT, ZoneOffset.UTC));

    private final MapService mapService;

    private final MapServiceOptionsSupplier optionsSupplier;

    private final boolean useTraffic;

    @Inject
    public TravelTimeMatrixEnricher(MapService mapService, MapServiceOptionsSupplier optionsSupplier,
            @ConfigProperty(name = "ai.timefold.platform.map-service.use-traffic", defaultValue = "false") Boolean useTraffic) {
        this.mapService = mapService;
        this.optionsSupplier = optionsSupplier;
        this.useTraffic = useTraffic;
    }

    @Retry(maxRetries = 5, delay = 1, delayUnit = ChronoUnit.SECONDS, abortOn = {
            TravelTimeAndDistanceConverterException.class,
            IllegalArgumentException.class,
            MapServiceIllegalArgumentException.class
    })
    @Override
    public LocationsAwareSolverModel<?> enrich(LocationsAwareSolverModel<?> solverModel) {
        // Three enricher paths:
        //   - TimeAwareLocationsSolverModel          -> per-timeframe matrices from the model's own availability.
        //   - LocationsAwareSolverModel + traffic on -> per-timeframe matrices, fetched by handing the map service a
        //     synthesized availability map that makes every location available across every timeframe.
        //   - LocationsAwareSolverModel + traffic off -> single matrix, scalar setters (keeps the
        //     IndexableDistanceMatrix index-cache optimization and location-set caching).
        // Timestamp-less lookups (getTravelTimeTo(other)) keep working on the traffic-on path because Location falls
        // back to a per-timeframe matrix when no single matrix is set.
        if (solverModel instanceof TimeAwareLocationsSolverModel<?> timeAwareSolverModel) {
            return enrichFromAvailability(timeAwareSolverModel, timeAwareSolverModel.getLocationsWithTimeAvailability());
        }
        if (useTraffic) {
            return enrichAllTimeframes(solverModel);
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
        return solverModel;
    }

    private LocationsAwareSolverModel<?> enrichAllTimeframes(LocationsAwareSolverModel<?> solverModel) {
        // No per-location availability on the plain path: make every location available across every timeframe by
        // mapping each to a full-day interval that overlaps all of them, then reuse the availability-based fetch.
        List<TimeInterval> fullCoverage = List.of(FULL_DAY);
        Map<Location, List<TimeInterval>> availability = new LinkedHashMap<>();
        for (Location location : solverModel.getLocations()) {
            availability.put(location, fullCoverage);
        }
        return enrichFromAvailability(solverModel, availability);
    }

    private LocationsAwareSolverModel<?> enrichFromAvailability(LocationsAwareSolverModel<?> solverModel,
            Map<Location, List<TimeInterval>> availability) {
        List<Location> locations = solverModel.getLocations();
        TravelTimesByAvailabilityWithMetadata result;
        try {
            result = mapService.getTravelTimeAndDistance(locations, optionsSupplier.getOptions(), availability);
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new TimefoldRuntimeException(ErrorCodes.MAP_SERVICE_UNKNOWN,
                    "Error getting travel time and distances from map service", e, false);
        }

        DistanceMatrix[] travelTimes = result.travelTimesByTimeframe();
        DistanceMatrix[] distances = result.distancesByTimeframe();
        if (travelTimes.length == 1) {
            // Single timeframe (e.g. traffic disabled, where the map service wraps one plain matrix as a one-bucket
            // array): stamp the scalar matrices so lookups use the IndexableDistanceMatrix index-cache fast path. The
            // time-aware overloads keep working because Location falls back to the single matrix when no per-timeframe
            // matrices are set.
            for (Location location : locations) {
                location.setTravelTimeMatrix(travelTimes[0]);
                location.setDistanceMatrix(distances[0]);
            }
        } else {
            for (Location location : locations) {
                location.setTravelTimeMatrices(travelTimes, result.timeframeIndexResolver());
                location.setDistanceMatrices(distances, result.timeframeIndexResolver());
            }
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
