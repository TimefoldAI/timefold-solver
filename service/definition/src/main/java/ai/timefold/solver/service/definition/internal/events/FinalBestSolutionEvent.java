package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Sent when the solver worker produces the final best solution upon termination conditions.
 */
public final class FinalBestSolutionEvent extends SolverWorkerEvent {

    private final SolverRunSummary runSummary;

    public FinalBestSolutionEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName) {
        this(metadata, model, job, planName, tenantName, null);
    }

    public FinalBestSolutionEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName,
            SolverRunSummary runSummary) {
        super(metadata, model, job, planName, tenantName, null);
        this.runSummary = runSummary;
    }

    /**
     * @return the summary of the solver run that produced the final best solution; null if not available
     */
    public SolverRunSummary getRunSummary() {
        return runSummary;
    }

}
