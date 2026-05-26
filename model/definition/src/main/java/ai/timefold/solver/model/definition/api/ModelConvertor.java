package ai.timefold.solver.model.definition.api;

import java.util.Optional;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.model.definition.api.domain.ModelConfig;
import ai.timefold.solver.model.definition.api.domain.ModelRequest;
import ai.timefold.solver.model.definition.api.domain.ModelResponse;

/**
 * The convertor of:
 * <ul>
 * <li>The <code>ModelInput</code> and the <code>ModelConfig</code> (usually obtained from the
 * {@link ModelRequest}) into the <code>SolverModel</code> usable by the Timefold Solver.</li>
 * <li>The <code>SolverModel</code> representing a planning solution into the <code>ModelOutput</code> (usually forming a part
 * of the {@link ModelResponse}).</li>
 * </ul>
 *
 * @param <Score_> The solver model concrete {@link ai.timefold.solver.core.api.domain.solution.PlanningScore} type.
 * @param <ModelInput_> The type of the model input part of the {@link ModelRequest}.
 * @param <ModelConfigurationOverrides_> The type of the model configuration overrides optionally specified in the
 *        {@link ModelRequest}.
 * @param <SolverModel_> The type of the {@link ai.timefold.solver.core.api.domain.solution.PlanningSolution} class to be used
 *        by Timefold solver.
 * @param <ModelOutput_> The type of the model output part of the {@link ModelResponse}.
 */
public non-sealed interface ModelConvertor<Score_ extends Score<Score_>, ModelInput_ extends ModelInput, ModelConfigurationOverrides_ extends ModelConfigOverrides, SolverModel_ extends SolverModel<Score_>, ModelOutput_ extends ModelOutput>
        extends ModelConvertorBase {

    /**
     * Converts the given {@link ModelInput_} and {@link ModelConfig} into a {@link SolverModel_} instance
     * usable by the Timefold solver.
     * <p>
     * Optionally, the {@code lastModelOutput} parameter can be provided to recover from a previous failure and restart
     * the solving process. In such cases, {@code lastModelOutput} represents the last stored output before the failure,
     * allowing the method to restore or reuse relevant state as needed.
     *
     * @param modelInput the model input part of the request, containing the data to be converted
     * @param modelConfig the model configuration, including any overrides to be applied
     * @param lastModelOutput an {@link Optional} containing the last stored model output before a failure, or empty if not
     *        recovering
     * @return the solver model instance to be used by the Timefold solver
     */
    SolverModel_ toSolverModel(ModelInput_ modelInput, ModelConfig<ModelConfigurationOverrides_> modelConfig,
            Optional<ModelOutput_> lastModelOutput);

    ModelOutput_ toModelOutput(SolverModel_ solverModel);

    /**
     * Applies changes from the given {@link ModelOutput_} to the given {@link ModelInput_}.
     * <p>
     * This method modifies the provided {@link ModelInput_} to create an updated {@link ModelInput_} that reflects
     * changes introduced by solving represented in the {@link ModelOutput_}.
     * <p>
     * The updated {@link ModelInput_} can be used as a basis for new datasets by applying external changes.
     *
     * @param modelInput The model input to be updated.
     * @param modelOutput The model output containing changes to be applied to the model input.
     * @return Reference to the updated {@link ModelInput_}.
     */
    ModelInput_ applyOutputToInput(ModelInput_ modelInput, ModelOutput_ modelOutput);
}
