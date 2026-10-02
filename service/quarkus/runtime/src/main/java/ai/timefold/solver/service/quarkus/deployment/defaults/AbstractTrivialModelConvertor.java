package ai.timefold.solver.service.quarkus.deployment.defaults;

import java.util.Objects;
import java.util.Optional;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelConvertor;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.ModelConfig;
import ai.timefold.solver.service.definition.api.domain.ModelRequest;
import ai.timefold.solver.service.definition.api.domain.ModelResponse;

import org.jspecify.annotations.NullMarked;

/**
 * Default model convertor for the trivial case when:
 * <ul>
 * <li><code>ModelInput</code> is the same as <code>ModelOutput</code> and <code>SolverModel</code>.</li>
 * <li>The argument <code>modelConfig</code> can be ignored during conversion.</li>
 * </ul>
 *
 * @param <Score_> The solver model concrete {@link ai.timefold.solver.core.api.domain.solution.PlanningScore} type.
 * @param <ModelInput_> The type of the model input part of the {@link ModelRequest}.
 * @param <ModelOutput_> The type of the model output part of the {@link ModelResponse}.
 * @param <ModelConfigurationOverrides_> The type of the model configuration overrides optionally specified in the
 *        {@link ModelRequest}.
 * @param <SolverModel_> The type of the {@link ai.timefold.solver.core.api.domain.solution.PlanningSolution} class to be used
 *        by Timefold solver.
 */
@NullMarked
public abstract class AbstractTrivialModelConvertor<Score_ extends Score<Score_>, ModelInput_ extends ModelInput, ModelConfigurationOverrides_ extends ModelConfigOverrides, SolverModel_ extends SolverModel<Score_>, ModelOutput_ extends ModelOutput>
        implements
        ModelConvertor<Score_, ModelInput_, ModelConfigurationOverrides_, SolverModel_, ModelOutput_> {

    @Override
    public SolverModel_ toSolverModel(ModelInput_ modelInput, ModelConfig<ModelConfigurationOverrides_> modelConfig,
            Optional<ModelOutput_> lastModelOutput) {
        Objects.requireNonNull(modelInput, "modelInput");
        Objects.requireNonNull(modelConfig, "modelConfig");
        Objects.requireNonNull(lastModelOutput, "lastModelOutput");
        if (lastModelOutput.isPresent() && !(lastModelOutput.get().getClass()).equals(modelInput.getClass())) {
            throw new IllegalArgumentException(
                    "Trivial conversion is possible only when modelInput (%s) and modelOutput (%s) are of the same class."
                            .formatted(modelInput.getClass(), lastModelOutput.get().getClass()));
        }
        return lastModelOutput.isPresent() ? (SolverModel_) lastModelOutput.get() : (SolverModel_) modelInput;
    }

    @Override
    public ModelOutput_ toModelOutput(SolverModel_ solverModel) {
        Objects.requireNonNull(solverModel, "solverModel");
        return (ModelOutput_) solverModel;
    }

    @Override
    public ModelInput_ applyOutputToInput(ModelInput_ modelInput, ModelOutput_ modelOutput) {
        Objects.requireNonNull(modelInput, "modelInput");
        Objects.requireNonNull(modelOutput, "modelOutput");
        if (!modelOutput.getClass().equals(modelInput.getClass())) {
            throw new IllegalArgumentException(
                    "Trivial conversion is possible only when modelInput (%s) and modelOutput (%s) are of the same class."
                            .formatted(modelInput.getClass(), modelOutput.getClass()));
        }

        return (ModelInput_) modelOutput;
    }
}
