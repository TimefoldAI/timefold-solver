package ai.timefold.solver.service.definition.internal.events;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.service.definition.api.ModelPostProcessingResult;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Sent when the solver worker produces the final best solution upon termination conditions.
 */
public final class FinalBestSolutionEvent extends SolverWorkerEvent {

    private final List<ModelPostProcessingResult> postProcessingResults;

    public FinalBestSolutionEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName) {
        this(metadata, model, job, planName, tenantName, null);
    }

    public FinalBestSolutionEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName,
            List<ModelPostProcessingResult> postProcessingResults) {
        super(metadata, model, job, planName, tenantName, null);
        this.postProcessingResults = postProcessingResults == null ? List.of() : List.copyOf(postProcessingResults);
    }

    /**
     * The results contributed by the post-processors for this run, in the order the post-processors ran.
     *
     * @return the post-processor results, never {@code null}
     */
    public List<ModelPostProcessingResult> getPostProcessingResults() {
        return postProcessingResults;
    }

}
