package ai.timefold.solver.service.maps.service.client.impl.processors;

import java.util.List;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.ModelPostProcessingResult;
import ai.timefold.solver.service.definition.api.ModelPostProcessor;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.maps.api.model.Waypoints;
import ai.timefold.solver.service.maps.service.integration.impl.WaypointsService;

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

    @Override
    public ModelPostProcessingResult process(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        List<Waypoints> waypoints = waypointsService.getWaypoints(id, Set.of());

        if (waypoints != null && !waypoints.isEmpty()) {
            storageService.storeWaypoints(id, waypoints);
        }

        return null;
    }

    @Override
    public void processComputed(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        // no op - don't calculate way points on computed only
    }
}
