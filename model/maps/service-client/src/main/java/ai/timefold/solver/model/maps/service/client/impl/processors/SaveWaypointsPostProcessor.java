package ai.timefold.solver.model.maps.service.client.impl.processors;

import java.util.List;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.api.ModelOutput;
import ai.timefold.solver.model.definition.api.ModelPostProcessor;
import ai.timefold.solver.model.definition.api.SolverModel;
import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.model.maps.api.model.Waypoints;
import ai.timefold.solver.model.maps.service.integration.impl.WaypointsService;

@Priority(value = jakarta.interceptor.Interceptor.Priority.APPLICATION)
@ApplicationScoped
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
        }
    }

    @Override
    public void processComputed(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        // no op - don't calculate way points on computed only
    }
}
