package ai.timefold.solver.model.definition.internal.events;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.model.definition.api.SolverModel;
import ai.timefold.solver.model.definition.api.domain.Metadata;

/**
 * This event is sent when a new best solution is found during the solving process.
 * <p>
 * It can be used to monitor the progress of the solver and to take actions based on the best solution found so far.
 */
public final class BestSolutionEvent extends SolverWorkerEvent {

    public BestSolutionEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName,
            String eventProducerId) {
        super(metadata, model, job, planName, tenantName, eventProducerId);
    }

}
