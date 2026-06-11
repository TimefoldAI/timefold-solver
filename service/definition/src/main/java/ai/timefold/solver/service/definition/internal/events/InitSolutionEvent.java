package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Sent when a solver worker produces the first, initialized, solution.
 */
public final class InitSolutionEvent extends SolverWorkerEvent {

    public InitSolutionEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName,
            String eventProducerId) {
        super(metadata, model, job, planName, tenantName, eventProducerId);
    }

}
