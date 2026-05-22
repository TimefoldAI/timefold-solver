package ai.timefold.solver.model.maps.service.client.impl;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.api.SolverModel;
import ai.timefold.solver.model.definition.internal.error.ErrorCodes;
import ai.timefold.solver.model.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.model.definition.internal.events.BestSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.FinalBestSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.InitSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.SolverChannels;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.api.model.Waypoints;
import ai.timefold.solver.model.maps.service.client.api.MapService;
import ai.timefold.solver.model.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.model.maps.service.integration.api.WaypointsExtractor;
import ai.timefold.solver.model.maps.service.integration.api.WaypointsExtractorBase;
import ai.timefold.solver.model.maps.service.integration.impl.WaypointsService;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "unchecked", "rawtypes" })
@ApplicationScoped
public class WaypointsServiceImpl implements WaypointsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaypointsServiceImpl.class);

    private final WaypointsExtractor waypointsExtractor;

    private final MapService mapService;

    private final MapServiceOptionsSupplier optionsSupplier;

    private final Map<String, Map<String, Waypoints>> runIdsToWaypoints = new ConcurrentHashMap<>();

    @Inject
    public WaypointsServiceImpl(MapService mapService,
            Instance<WaypointsExtractorBase> waypointsExtractor,
            MapServiceOptionsSupplier optionsSupplier) {
        this.waypointsExtractor = waypointsExtractor.isResolvable() ? (WaypointsExtractor) waypointsExtractor.get() : null;
        this.mapService = mapService;
        this.optionsSupplier = optionsSupplier;
    }

    @Override
    public synchronized List<Waypoints> getWaypoints(String runId, Set<String> objectIds) {

        if (this.waypointsExtractor == null) {
            return List.of();
        }

        Map<String, Waypoints> calculated = this.runIdsToWaypoints.get(runId);

        if (calculated == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Unable to find data set for id " + runId);
        }

        LOGGER.debug("Run {} has returned {} waypoints to be calculated with route information with filter by object id {}",
                runId, calculated.size(), objectIds);
        for (Entry<String, Waypoints> waypointEntry : calculated.entrySet()) {
            if (!waypointEntry.getValue().calculated()
                    && (objectIds.isEmpty() || objectIds.contains(waypointEntry.getKey()))) {
                LOGGER.debug("Calculating waypoints for {} for run {}", waypointEntry.getValue().id(), runId);
                Waypoints calculatedWaypoint = calculateWaypoints(waypointEntry.getValue());
                LOGGER.debug("Successfully calculated waypoints for {} for run {}", waypointEntry.getValue().id(), runId);
                calculated.put(waypointEntry.getKey(), calculatedWaypoint);
            }
        }

        return calculated.values().stream().filter(item -> objectIds.isEmpty() || objectIds.contains(item.id())).toList();
    }

    @Retry(maxRetries = 5, delay = 1, delayUnit = ChronoUnit.SECONDS, abortOn = {
            IllegalArgumentException.class,
            MapServiceIllegalArgumentException.class
    })
    public Waypoints calculateWaypoints(Waypoints waypoints) {
        if (this.waypointsExtractor == null) {
            return waypoints;
        }
        Collection<Location> locations;

        try {
            locations = mapService.getWaypoints(waypoints.waypoints(), optionsSupplier.getOptions());
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Wrap in a non-recoverable error
            throw new TimefoldRuntimeException(ErrorCodes.MAP_SERVICE_UNKNOWN,
                    "Error calculating waypoints from map service", e, false);
        }
        return new Waypoints(waypoints.id(), locations.stream().toList(), true);
    }

    @Incoming(SolverChannels.INIT_SOLUTION)
    public void onInitSolution(InitSolutionEvent event) {
        rebuildBaseWaypoints(event.getId(), event.getModel());
    }

    @Incoming(SolverChannels.BEST_SOLUTION)
    public void onBestSolution(BestSolutionEvent event) {
        rebuildBaseWaypoints(event.getId(), event.getModel());
    }

    @Incoming(SolverChannels.FINAL_BEST_SOLUTION)
    public void onFinalBestSolution(FinalBestSolutionEvent event) {
        rebuildBaseWaypoints(event.getId(), event.getModel());
    }

    private void rebuildBaseWaypoints(String runId, SolverModel solverModel) {
        if (this.waypointsExtractor != null && solverModel != null) {
            this.runIdsToWaypoints.compute(runId, (key, value) -> {
                LOGGER.debug("No waypoints have been calculated for run {}, extracting what should be calculated", runId);
                List<Waypoints> waypoints = waypointsExtractor.extractBaseWaypoints(solverModel);
                Map<String, Waypoints> map = new LinkedHashMap<>();

                waypoints.forEach(item -> map.put(item.id(), item));

                return map;
            });
        }
    }
}
