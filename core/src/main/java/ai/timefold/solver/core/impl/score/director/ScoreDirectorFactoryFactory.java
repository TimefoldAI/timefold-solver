package ai.timefold.solver.core.impl.score.director;

import java.util.ArrayList;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.incremental.IncrementalScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class ScoreDirectorFactoryFactory<Solution_, Score_ extends Score<Score_>> {

    private final ScoreDirectorFactoryConfig config;

    public ScoreDirectorFactoryFactory(ScoreDirectorFactoryConfig config) {
        this.config = config;
    }

    public ScoreDirectorFactory<Solution_, Score_> buildScoreDirectorFactory(EnvironmentMode environmentMode,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var scoreDirectorFactory = decideMultipleScoreDirectorFactories(solutionDescriptor, environmentMode);
        var assertionScoreDirectorFactory = config.getAssertionScoreDirectorFactory();
        if (assertionScoreDirectorFactory != null) {
            if (assertionScoreDirectorFactory.getAssertionScoreDirectorFactory() != null) {
                throw new IllegalArgumentException(
                        "A assertionScoreDirectorFactory (%s) cannot have a non-null assertionScoreDirectorFactory (%s)."
                                .formatted(assertionScoreDirectorFactory,
                                        assertionScoreDirectorFactory.getAssertionScoreDirectorFactory()));
            }
            if (environmentMode.compareTo(EnvironmentMode.STEP_ASSERT) > 0) {
                throw new IllegalArgumentException(
                        "A non-null assertionScoreDirectorFactory (%s) requires an environmentMode (%s) of %s or lower."
                                .formatted(assertionScoreDirectorFactory, environmentMode, EnvironmentMode.STEP_ASSERT));
            }
            var assertionScoreDirectorFactoryFactory =
                    new ScoreDirectorFactoryFactory<Solution_, Score_>(assertionScoreDirectorFactory);
            scoreDirectorFactory.setAssertionScoreDirectorFactory(assertionScoreDirectorFactoryFactory
                    .buildScoreDirectorFactory(EnvironmentMode.NON_REPRODUCIBLE, solutionDescriptor));
        }
        scoreDirectorFactory.setInitializingScoreTrend(InitializingScoreTrend.parseTrend(
                config.getInitializingScoreTrend() == null ? InitializingScoreTrendLevel.ANY.name()
                        : config.getInitializingScoreTrend(),
                solutionDescriptor.getScoreDefinition().getLevelsSize()));
        if (environmentMode.isFullyAsserted()) {
            scoreDirectorFactory.setAssertClonedSolution(true);
        }
        if (environmentMode.isTracking()) {
            scoreDirectorFactory.setTrackingWorkingSolution(true);
        }
        return scoreDirectorFactory;
    }

    protected AbstractScoreDirectorFactory<Solution_, Score_, ?> decideMultipleScoreDirectorFactories(
            SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentMode) {
        if (!ConfigUtils.isEmptyCollection(config.getScoreDrlList())) {
            throw new IllegalStateException(
                    """
                            DRL constraints requested via scoreDrlList (%s), but this is no longer supported in Timefold Solver 0.9 and later.
                            Maybe upgrade from scoreDRL to ConstraintStreams using this recipe: https://timefold.ai/blog/migrating-score-drl-to-constraint-streams"""
                            .formatted(config.getScoreDrlList()));
        }
        assertCorrectDirectorFactory(config);

        // At this point, we are guaranteed to have at most one score director factory selected.
        if (config.getEasyScoreCalculatorClass() != null) {
            return EasyScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor, config);
        } else if (config.getIncrementalScoreCalculatorClass() != null) {
            return IncrementalScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor, config);
        } else if (config.getConstraintProviderClass() != null) {
            return BavetConstraintStreamScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor, config,
                    environmentMode);
        } else {
            throw new IllegalArgumentException(
                    "The scoreDirectorFactory lacks configuration for either constraintProviderClass, " +
                            "easyScoreCalculatorClass or incrementalScoreCalculatorClass.");
        }
    }

    private static void assertCorrectDirectorFactory(ScoreDirectorFactoryConfig config) {
        var easyScoreCalculatorClass = config.getEasyScoreCalculatorClass();
        var hasEasyScoreCalculator = easyScoreCalculatorClass != null;
        if (!hasEasyScoreCalculator && config.getEasyScoreCalculatorCustomProperties() != null) {
            throw new IllegalStateException(
                    "If there is no easyScoreCalculatorClass (%s), then there can be no easyScoreCalculatorCustomProperties (%s) either."
                            .formatted(easyScoreCalculatorClass, config.getEasyScoreCalculatorCustomProperties()));
        }
        var incrementalScoreCalculatorClass = config.getIncrementalScoreCalculatorClass();
        var hasIncrementalScoreCalculator = incrementalScoreCalculatorClass != null;
        if (!hasIncrementalScoreCalculator && config.getIncrementalScoreCalculatorCustomProperties() != null) {
            throw new IllegalStateException(
                    "If there is no incrementalScoreCalculatorClass (%s), then there can be no incrementalScoreCalculatorCustomProperties (%s) either."
                            .formatted(incrementalScoreCalculatorClass,
                                    config.getIncrementalScoreCalculatorCustomProperties()));
        }
        var constraintProviderClass = config.getConstraintProviderClass();
        var hasConstraintProvider = constraintProviderClass != null;
        if (!hasConstraintProvider && config.getConstraintProviderCustomProperties() != null) {
            throw new IllegalStateException(
                    "If there is no constraintProviderClass (%s), then there can be no constraintProviderCustomProperties (%s) either."
                            .formatted(constraintProviderClass, config.getConstraintProviderCustomProperties()));
        }
        if (hasEasyScoreCalculator && (hasIncrementalScoreCalculator || hasConstraintProvider)
                || (hasIncrementalScoreCalculator && hasConstraintProvider)) {
            var scoreDirectorFactoryPropertyList = new ArrayList<String>(3);
            if (hasEasyScoreCalculator) {
                scoreDirectorFactoryPropertyList
                        .add("an easyScoreCalculatorClass (%s)".formatted(easyScoreCalculatorClass.getName()));
            }
            if (hasConstraintProvider) {
                scoreDirectorFactoryPropertyList
                        .add("an constraintProviderClass (%s)".formatted(constraintProviderClass.getName()));
            }
            if (hasIncrementalScoreCalculator) {
                scoreDirectorFactoryPropertyList.add("an incrementalScoreCalculatorClass (%s)"
                        .formatted(incrementalScoreCalculatorClass.getName()));
            }
            var joined = String.join(" and ", scoreDirectorFactoryPropertyList);
            throw new IllegalArgumentException("The scoreDirectorFactory cannot have %s together."
                    .formatted(joined));
        }
    }

}
