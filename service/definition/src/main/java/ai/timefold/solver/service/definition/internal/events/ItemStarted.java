package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Event sent when the solver worker starts solving the dataset.
 */
public final class ItemStarted extends SolverWorkerEvent {

    public ItemStarted(Metadata metadata, SolverModel solverModel, String planName, String tenantName) {
        super(metadata, solverModel, null, planName, tenantName, null);
    }

}
