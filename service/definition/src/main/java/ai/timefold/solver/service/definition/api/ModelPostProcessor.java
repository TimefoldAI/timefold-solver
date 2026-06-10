package ai.timefold.solver.service.definition.api;

import org.jspecify.annotations.Nullable;

public interface ModelPostProcessor {

    /**
     * Process a successfully solved model run and optionally return a result.
     * <p>
     * The post processor is called when solver terminates either by meeting termination conditions or by termination request.
     *
     * @param modelOutput the model output
     * @param solverModel the model with all data taken from the solving
     * @param id unique identifier of the model
     * @return the result of the post processor, or {@code null} if this post-processor contributes none
     */
    @Nullable
    ModelPostProcessingResult process(ModelOutput modelOutput, SolverModel<?> solverModel, String id);

    /**
     * Process successfully computed model (not yet solved).
     * <p>
     * The post processor is called when dataset is validated and its initial outputs computed.
     *
     * @param solverModel the model
     * @param id unique identifier of the model
     */
    default void processComputed(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        process(modelOutput, solverModel, id);
    }

    /**
     * Process failed model run
     *
     * @param id unique identifier of the user model
     * @param error the error that caused the solving failure
     */
    default void processFailed(String id, Throwable error) {
    }
}
