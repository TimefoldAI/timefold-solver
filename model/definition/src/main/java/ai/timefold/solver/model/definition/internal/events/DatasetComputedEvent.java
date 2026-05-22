package ai.timefold.solver.model.definition.internal.events;

import ai.timefold.solver.model.definition.api.SolverModel;
import ai.timefold.solver.model.definition.api.domain.Metadata;

/**
 * Event sent when the outputs of a dataset are computed before solving.
 * This happens after the {@link DatasetValidatedEvent} and before the {@link SolveStartCommand}.
 */
public final class DatasetComputedEvent extends SolverWorkerEvent {

    public DatasetComputedEvent(Metadata metadata, SolverModel solverModel, String planName, String tenantName) {
        super(metadata, solverModel, null, planName, tenantName, null);
    }
}
