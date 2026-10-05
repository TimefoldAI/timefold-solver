package ai.timefold.solver.service.maps.service.client.impl;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import ai.timefold.solver.service.definition.internal.events.ItemCompleted;
import ai.timefold.solver.service.definition.internal.events.ItemFailed;
import ai.timefold.solver.service.definition.internal.events.ItemStarted;
import ai.timefold.solver.service.definition.internal.events.SolverChannels;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.Waypoints;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;
import ai.timefold.solver.service.maps.service.integration.api.WaypointsExtractor;
import ai.timefold.solver.service.maps.service.integration.api.WaypointsExtractorBase;
import ai.timefold.solver.service.maps.service.integration.impl.WaypointsService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.runtime.Startup;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Startup
@ApplicationScoped
public class WaypointsServiceImpl implements WaypointsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaypointsServiceImpl.class);

    private static final TypeReference<List<Waypoints>> WAYPOINTS_LIST_TYPE = new TypeReference<>() {
    };

    private static final int FINISHED_RUN_ID_CAPACITY = 1024;

    private final WaypointsExtractor waypointsExtractor;

    private final MapService mapService;

    private final MapServiceOptionsSupplier optionsSupplier;

    private final AbstractStorageService<?, ?, ?, ?, ?, ?, ?> storageService;

    private final ExecutorService waypointsExecutor;

    private final Map<String, RunRoutes> runRoutesMap = new ConcurrentHashMap<>();

    private final Set<String> finishedRunIdSet = Collections.synchronizedSet(Collections.newSetFromMap(
            new LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldestEntry) {
                    return size() > FINISHED_RUN_ID_CAPACITY;
                }
            }));

    /**
     * Creates the service with its own pool for map-service waypoints calls.
     *
     * @param mapService the map service that calculates the waypoints of a route
     * @param waypointsExtractor the extractor of the base waypoints from a solver model, if one exists
     * @param optionsSupplier the supplier of the map-service options
     * @param storageService the storage that serves the stored waypoints of a finished run
     * @param waypointsParallelism the maximum number of concurrent map-service waypoints calls
     * @throws IllegalArgumentException if waypointsParallelism is not positive
     */
    @Inject
    public WaypointsServiceImpl(MapService mapService,
            Instance<WaypointsExtractorBase> waypointsExtractor,
            MapServiceOptionsSupplier optionsSupplier,
            AbstractStorageService<?, ?, ?, ?, ?, ?, ?> storageService,
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
        this.storageService = storageService;
        this.waypointsExecutor = Executors.newFixedThreadPool(waypointsParallelism,
                Thread.ofPlatform().name("waypoints-fetch-", 0).daemon().factory());
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
            return getStoredWaypoints(runId, objectIds);
        }
        runRoutes.markViewed();
        var waypointsFutureList = runRoutes.getRouteList(objectIds).stream().map(this::fetchOnce).toList();
        return waypointsFutureList.stream().map(WaypointsServiceImpl::await).toList();
    }

    private List<Waypoints> getStoredWaypoints(String runId, Set<String> objectIds) {
        List<Waypoints> storedWaypointsList = storageService.getWaypoints(runId, WAYPOINTS_LIST_TYPE);
        if (storedWaypointsList == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Unable to find data set for id " + runId);
        }
        return storedWaypointsList.stream()
                .filter(waypoints -> objectIds.isEmpty() || objectIds.contains(waypoints.id()))
                .toList();
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

    @Incoming(SolverChannels.STARTED)
    public void onStarted(ItemStarted event) {
        finishedRunIdSet.remove(event.getId());
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

    @Incoming(SolverChannels.COMPLETED)
    public void onCompleted(ItemCompleted event) {
        releaseFinishedRun(event.getId());
    }

    @Incoming(SolverChannels.FAILED)
    public void onFailed(ItemFailed event) {
        releaseFinishedRun(event.getId());
    }

    private void releaseFinishedRun(String runId) {
        finishedRunIdSet.add(runId);
        runRoutesMap.remove(runId);
    }

    private void rebuildBaseWaypoints(String runId, SolverModel solverModel) {
        if (this.waypointsExtractor == null || solverModel == null) {
            return;
        }
        List<Waypoints> baseWaypointsList = waypointsExtractor.extractBaseWaypoints(solverModel);
        var runRoutes = runRoutesMap.computeIfAbsent(runId, id -> finishedRunIdSet.contains(id) ? null : new RunRoutes());
        if (runRoutes == null) {
            return;
        }
        runRoutes.replaceRoutes(baseWaypointsList);
        if (runRoutes.isViewed()) {
            runRoutes.requestBackgroundFetch();
            startBackgroundFetchIfIdle(runId, runRoutes);
        }
    }

    private void startBackgroundFetchIfIdle(String runId, RunRoutes runRoutes) {
        if (!runRoutes.tryStartBackgroundFetch()) {
            return;
        }
        var isCompletionAttached = false;
        try {
            fetchAllRoutes(runRoutes).whenComplete((ignored, failure) -> onBackgroundFetchComplete(runId, runRoutes, failure));
            isCompletionAttached = true;
        } catch (RuntimeException e) {
            logBackgroundFetchFailure("failed to start", runId, e);
        } finally {
            if (!isCompletionAttached) {
                runRoutes.finishBackgroundFetch();
            }
        }
    }

    private CompletableFuture<Void> fetchAllRoutes(RunRoutes runRoutes) {
        var waypointsFutureArray = runRoutes.getRouteList(Set.of()).stream()
                .map(this::fetchOnce)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(waypointsFutureArray);
    }

    private void onBackgroundFetchComplete(String runId, RunRoutes runRoutes, Throwable failure) {
        if (failure != null) {
            logBackgroundFetchFailure("failed", runId, failure);
        }
        runRoutes.finishBackgroundFetch();
        if (runRoutes.isBackgroundFetchRequested() && runRoutesMap.get(runId) == runRoutes) {
            startBackgroundFetchIfIdle(runId, runRoutes);
        }
    }

    private static void logBackgroundFetchFailure(String outcome, String runId, Throwable failure) {
        LOGGER.warn("Background waypoints fetch {} for run {}: {}. The next request retries.",
                outcome, runId, failure.getMessage());
        LOGGER.debug("Stack trace of the background waypoints fetch failure for run {}.", runId, failure);
    }
}
