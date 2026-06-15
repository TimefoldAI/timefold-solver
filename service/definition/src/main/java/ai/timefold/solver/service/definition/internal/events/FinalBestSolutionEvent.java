package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Sent when the solver worker produces the final best solution upon termination conditions.
 */
public final class FinalBestSolutionEvent extends SolverWorkerEvent {

    public FinalBestSolutionEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName) {
        super(metadata, model, job, planName, tenantName, null);
    }

}
