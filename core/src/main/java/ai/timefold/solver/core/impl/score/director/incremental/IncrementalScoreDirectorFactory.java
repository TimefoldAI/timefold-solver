package ai.timefold.solver.core.impl.score.director.incremental;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;

/**
 * Incremental implementation of {@link ScoreDirectorFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see IncrementalScoreDirector
 * @see ScoreDirectorFactory
 */
public final class IncrementalScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirectorFactory<Solution_, Score_> {

    public static <Solution_, Score_ extends Score<Score_>> IncrementalScoreDirectorFactory<Solution_, Score_>
            buildScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor, ScoreDirectorFactoryConfig config) {
        if (!IncrementalScoreCalculator.class.isAssignableFrom(config.getIncrementalScoreCalculatorClass())) {
            throw new IllegalArgumentException(
                    "The incrementalScoreCalculatorClass (%s) does not implement %s."
                            .formatted(config.getIncrementalScoreCalculatorClass(),
                                    IncrementalScoreCalculator.class.getSimpleName()));
        }
        return new IncrementalScoreDirectorFactory<>(solutionDescriptor, () -> {
            IncrementalScoreCalculator<Solution_, Score_> incrementalScoreCalculator = ConfigUtils.newInstance(config,
                    "incrementalScoreCalculatorClass", config.getIncrementalScoreCalculatorClass());
            ConfigUtils.applyCustomProperties(incrementalScoreCalculator, "incrementalScoreCalculatorClass",
                    config.getIncrementalScoreCalculatorCustomProperties(), "incrementalScoreCalculatorCustomProperties");
            return incrementalScoreCalculator;
        });
    }

    private final Supplier<IncrementalScoreCalculator<Solution_, Score_>> incrementalScoreCalculatorSupplier;

    public IncrementalScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            Supplier<IncrementalScoreCalculator<Solution_, Score_>> incrementalScoreCalculatorSupplier) {
        super(solutionDescriptor);
        this.incrementalScoreCalculatorSupplier = incrementalScoreCalculatorSupplier;
    }

    @Override
    public boolean supportsConstraintMatching() {
        return incrementalScoreCalculatorSupplier.get() instanceof ConstraintMatchAwareIncrementalScoreCalculator;
    }

    @Override
    public IncrementalScoreDirector<Solution_, Score_> buildScoreDirector(boolean lookUpEnabled,
            boolean constraintMatchEnabledPreference, boolean expectShadowVariablesInCorrectState) {
        return new IncrementalScoreDirector<>(this,
                lookUpEnabled, constraintMatchEnabledPreference, expectShadowVariablesInCorrectState,
                incrementalScoreCalculatorSupplier.get());
    }

}
