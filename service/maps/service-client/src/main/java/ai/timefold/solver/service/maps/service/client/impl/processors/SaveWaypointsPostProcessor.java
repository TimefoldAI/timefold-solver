package ai.timefold.solver.service.maps.service.client.impl.processors;

import java.util.List;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.ModelPostProcessor;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.maps.api.model.Waypoints;
import ai.timefold.solver.service.maps.service.integration.impl.WaypointsService;

@Priority(value = Interceptor.Priority.APPLICATION)
// ArC rewrites an @ApplicationScoped bean with only an injection constructor, and JaCoCo then loses it.
@Singleton
public class SaveWaypointsPostProcessor implements ModelPostProcessor {

    private AbstractStorageService<?, ?, ?, ?, ?, ?, ?> storageService;

    private WaypointsService waypointsService;

    @Inject
    public SaveWaypointsPostProcessor(AbstractStorageService<?, ?, ?, ?, ?, ?, ?> storageService,
            WaypointsService waypointsService) {
        this.storageService = storageService;
        this.waypointsService = waypointsService;
    }

    public void process(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {

        List<Waypoints> waypoints = waypointsService.getWaypoints(id, Set.of());

        if (waypoints != null && !waypoints.isEmpty()) {
            storageService.storeWaypoints(id, waypoints);
            // Not in a finally block: a run released before its waypoints are stored returns 404 until a restart.
            waypointsService.releaseFinishedRun(id);
        }
    }

    @Override
    public void processComputed(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        // no op - don't calculate way points on computed only
    }
}
