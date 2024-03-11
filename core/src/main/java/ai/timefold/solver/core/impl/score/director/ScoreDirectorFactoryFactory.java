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

    public InnerScoreDirectorFactory<Solution_, Score_> buildScoreDirectorFactory(EnvironmentMode environmentMode,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var scoreDirectorFactory = decideMultipleScoreDirectorFactories(solutionDescriptor, environmentMode);
        if (config.getAssertionScoreDirectorFactory() != null) {
            if (config.getAssertionScoreDirectorFactory().getAssertionScoreDirectorFactory() != null) {
                throw new IllegalArgumentException("A assertionScoreDirectorFactory ("
                        + config.getAssertionScoreDirectorFactory() + ") cannot have a non-null assertionScoreDirectorFactory ("
                        + config.getAssertionScoreDirectorFactory().getAssertionScoreDirectorFactory() + ").");
            }
            if (environmentMode.compareTo(EnvironmentMode.FAST_ASSERT) > 0) {
                throw new IllegalArgumentException("A non-null assertionScoreDirectorFactory ("
                        + config.getAssertionScoreDirectorFactory() + ") requires an environmentMode ("
                        + environmentMode + ") of " + EnvironmentMode.FAST_ASSERT + " or lower.");
            }
            var assertionScoreDirectorFactoryFactory =
                    new ScoreDirectorFactoryFactory<Solution_, Score_>(config.getAssertionScoreDirectorFactory());
            scoreDirectorFactory.setAssertionScoreDirectorFactory(assertionScoreDirectorFactoryFactory
                    .buildScoreDirectorFactory(EnvironmentMode.NON_REPRODUCIBLE, solutionDescriptor));
        }
        scoreDirectorFactory.setInitializingScoreTrend(InitializingScoreTrend.parseTrend(
                config.getInitializingScoreTrend() == null ? InitializingScoreTrendLevel.ANY.name()
                        : config.getInitializingScoreTrend(),
                solutionDescriptor.getScoreDefinition().getLevelsSize()));
        if (environmentMode.isNonIntrusiveFullAsserted()) {
            scoreDirectorFactory.setAssertClonedSolution(true);
        }
        if (environmentMode.isTracking()) {
            scoreDirectorFactory.setTrackingWorkingSolution(true);
        }
        return scoreDirectorFactory;
    }

    protected AbstractScoreDirectorFactory<Solution_, Score_> decideMultipleScoreDirectorFactories(
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
        var hasEasyScoreCalculator = config.getEasyScoreCalculatorClass() != null;
        if (!hasEasyScoreCalculator && config.getEasyScoreCalculatorCustomProperties() != null) {
            throw new IllegalStateException(
                    "If there is no easyScoreCalculatorClass (%s), then there can be no easyScoreCalculatorCustomProperties (%s) either."
                            .formatted(config.getEasyScoreCalculatorClass(), config.getEasyScoreCalculatorCustomProperties()));
        }
        var hasIncrementalScoreCalculator = config.getIncrementalScoreCalculatorClass() != null;
        if (!hasIncrementalScoreCalculator && config.getIncrementalScoreCalculatorCustomProperties() != null) {
            throw new IllegalStateException(
                    "If there is no incrementalScoreCalculatorClass (%s), then there can be no incrementalScoreCalculatorCustomProperties (%s) either."
                            .formatted(config.getIncrementalScoreCalculatorClass(),
                                    config.getIncrementalScoreCalculatorCustomProperties()));
        }
        var hasConstraintProvider = config.getConstraintProviderClass() != null;
        if (!hasConstraintProvider && config.getConstraintProviderCustomProperties() != null) {
            throw new IllegalStateException(
                    "If there is no constraintProviderClass (%s), then there can be no constraintProviderCustomProperties (%s) either."
                            .formatted(config.getConstraintProviderClass(),
                                    config.getConstraintProviderCustomProperties()));
        }
        if (hasEasyScoreCalculator && (hasIncrementalScoreCalculator || hasConstraintProvider)
                || (hasIncrementalScoreCalculator && hasConstraintProvider)) {
            var scoreDirectorFactoryPropertyList = new ArrayList<String>(3);
            if (hasEasyScoreCalculator) {
                scoreDirectorFactoryPropertyList
                        .add("an easyScoreCalculatorClass (%s)".formatted(config.getEasyScoreCalculatorClass().getName()));
            }
            if (hasConstraintProvider) {
                scoreDirectorFactoryPropertyList
                        .add("an constraintProviderClass (%s)".formatted(config.getConstraintProviderClass().getName()));
            }
            if (hasIncrementalScoreCalculator) {
                scoreDirectorFactoryPropertyList.add("an incrementalScoreCalculatorClass (%s)"
                        .formatted(config.getIncrementalScoreCalculatorClass().getName()));
            }
            var joined = String.join(" and ", scoreDirectorFactoryPropertyList);
            throw new IllegalArgumentException("The scoreDirectorFactory cannot have %s together."
                    .formatted(joined));
        }
    }

}
