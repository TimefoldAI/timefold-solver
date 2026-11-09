package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.service.definition.spi.model.SolverModel;
import ai.timefold.solver.service.definition.api.dto.dataset.Metadata;

/**
 * Sent when the solver worker produces the final best solution upon termination conditions.
 */
public final class FinalBestSolutionEvent extends SolverWorkerEvent {

    public FinalBestSolutionEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName) {
        super(metadata, model, job, planName, tenantName, null);
    }

}
