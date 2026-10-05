package ai.timefold.solver.service.maps.service.client.impl;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.service.definition.internal.events.BestSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.FinalBestSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.InitSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.SolverChannels;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.Waypoints;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.service.maps.service.integration.api.WaypointsExtractor;
import ai.timefold.solver.service.maps.service.integration.api.WaypointsExtractorBase;
import ai.timefold.solver.service.maps.service.integration.impl.WaypointsService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@SuppressWarnings({ "unchecked", "rawtypes" })
@ApplicationScoped
public class WaypointsServiceImpl implements WaypointsService {

    private final WaypointsExtractor waypointsExtractor;

    private final MapService mapService;

    private final MapServiceOptionsSupplier optionsSupplier;

    private final ManagedExecutor waypointsExecutor;

    private final Map<String, RunRoutes> runRoutesMap = new ConcurrentHashMap<>();

    /**
     * @param waypointsParallelism the maximum number of concurrent map-service waypoints calls
     */
    @Inject
    public WaypointsServiceImpl(MapService mapService,
            Instance<WaypointsExtractorBase> waypointsExtractor,
            MapServiceOptionsSupplier optionsSupplier,
            @ConfigProperty(name = "timefold.platform.map-service.waypoints-parallelism",
                    defaultValue = "8") int waypointsParallelism) {
        if (waypointsParallelism <= 0) {
            throw new IllegalArgumentException("""
                    The waypointsParallelism (%d) must be positive.
                    Maybe set timefold.platform.map-service.waypoints-parallelism to 1 or more."""
                    .formatted(waypointsParallelism));
        }
        this.waypointsExtractor = waypointsExtractor.isResolvable() ? (WaypointsExtractor) waypointsExtractor.get() : null;
        this.mapService = mapService;
        this.optionsSupplier = optionsSupplier;
        this.waypointsExecutor = ManagedExecutor.builder()
                .maxAsync(waypointsParallelism)
                .propagated(ThreadContext.NONE)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();
    }

    @PreDestroy
    void shutdownWaypointsExecutor() {
        waypointsExecutor.shutdownNow();
    }

    @Override
    public List<Waypoints> getWaypoints(String runId, Set<String> objectIds) {
        if (this.waypointsExtractor == null) {
            return List.of();
        }
        var runRoutes = runRoutesMap.get(runId);
        if (runRoutes == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Unable to find data set for id " + runId);
        }
        runRoutes.markViewed();
        var waypointsFutureList = runRoutes.getRouteList(objectIds).stream().map(this::fetchOnce).toList();
        return waypointsFutureList.stream().map(WaypointsServiceImpl::await).toList();
    }

    private CompletableFuture<Waypoints> fetchOnce(VehicleRoute vehicleRoute) {
        return vehicleRoute.fetchOnce(() -> CompletableFuture
                .supplyAsync(() -> calculateWaypoints(vehicleRoute.getBaseWaypoints()), waypointsExecutor));
    }

    private static Waypoints await(CompletableFuture<Waypoints> waypointsFuture) {
        try {
            return waypointsFuture.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
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
            List<Waypoints> baseWaypointsList = waypointsExtractor.extractBaseWaypoints(solverModel);
            runRoutesMap.computeIfAbsent(runId, id -> new RunRoutes()).replaceRoutes(baseWaypointsList);
        }
    }
}
