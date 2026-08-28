package ai.timefold.solver.core.impl.score.director;

import java.util.ArrayList;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.incremental.IncrementalScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.impl.solver.EnvironmentModeUtil;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Turns a {@link ScoreDirectorFactoryConfig} into the {@link ScoreDirectorFactory} it describes.
 * Note that this is a builder,
 * not a {@link ScoreDirectorFactory} itself.
 * <p>
 * It validates the config,
 * picks the one score director implementation the config selected:
 * ({@link EasyScoreDirectorFactory}, {@link IncrementalScoreDirectorFactory} or
 * {@link BavetConstraintStreamScoreDirectorFactory}).
 * <p>
 * A score director factory is built for exactly one {@link EnvironmentMode}.
 * A solver config whose phases all run in the solver's own mode therefore needs only one,
 * and gets the concrete factory.
 * As soon as a phase runs in another mode,
 * {@link #buildScoreDirectorFactory(EnvironmentMode, SolutionDescriptor)} returns a
 * {@link MultiEnvironmentScoreDirectorFactory} instead,
 * which serves that phase a factory of its own.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type to go with the solution
 */
@NullMarked
public class ScoreDirectorFactoryFactory<Solution_, Score_ extends Score<Score_>> {

    private final ScoreDirectorFactoryConfig scoreDirectorFactoryConfig;
    private final boolean useMultipleEnvironmentModes;

    public ScoreDirectorFactoryFactory(SolverConfig solverConfig) {
        this(solverConfig,
                Objects.requireNonNullElseGet(solverConfig.getScoreDirectorFactoryConfig(), ScoreDirectorFactoryConfig::new));
    }

    public ScoreDirectorFactoryFactory(ScoreDirectorFactoryConfig scoreDirectorFactoryConfig) {
        this(null, scoreDirectorFactoryConfig);
    }

    private ScoreDirectorFactoryFactory(@Nullable SolverConfig solverConfig,
            ScoreDirectorFactoryConfig scoreDirectorFactoryConfig) {
        this.scoreDirectorFactoryConfig = Objects.requireNonNull(scoreDirectorFactoryConfig);
        assertCorrectDirectorFactory(scoreDirectorFactoryConfig);
        // Without a solver config there are no phases, and therefore only ever the one environment mode asked for.
        this.useMultipleEnvironmentModes = solverConfig != null && needsMultipleEnvironmentModes(solverConfig);
    }

    /**
     * Whether some phase runs in an environment mode other than the solver's,
     * which is what makes the solver need more than one score director factory.
     * <p>
     * Note that a phase which does not override the mode contributes the solver's own,
     * so this is false when no phase overrides it, and true as soon as one does.
     * A phase which restates the solver's own mode is not an override in this sense.
     */
    private static boolean needsMultipleEnvironmentModes(SolverConfig solverConfig) {
        var globalEnvironmentMode = EnvironmentModeUtil.resolve(solverConfig);
        return EnvironmentModeUtil.resolvePhases(solverConfig, true).stream()
                .anyMatch(phaseEnvironmentMode -> phaseEnvironmentMode != globalEnvironmentMode);
    }

    /**
     * @return a factory for the given environment mode; a {@link MultiEnvironmentScoreDirectorFactory} when the
     *         config has a phase running in another mode, so that the phase can be served a factory of its own
     */
    public ScoreDirectorFactory<Solution_, Score_> buildScoreDirectorFactory(EnvironmentMode environmentMode,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var factory = buildConcreteScoreDirectorFactory(environmentMode, solutionDescriptor);
        if (useMultipleEnvironmentModes) {
            return new MultiEnvironmentScoreDirectorFactory<>(this, factory, environmentMode);
        }
        return factory;
    }

    /**
     * The factory the configured score director implies and never decorated.
     * {@link MultiEnvironmentScoreDirectorFactory} calls this for each further mode it has to serve,
     * so that its cache holds concrete factories rather than decorators.
     */
    AbstractScoreDirectorFactory<Solution_, Score_, ?> buildConcreteScoreDirectorFactory(EnvironmentMode environmentMode,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var factory = decideScoreDirectorFactory(solutionDescriptor, environmentMode);
        var assertionScoreDirectorFactoryConfig = scoreDirectorFactoryConfig.getAssertionScoreDirectorFactory();
        if (assertionScoreDirectorFactoryConfig != null) {
            if (assertionScoreDirectorFactoryConfig.getAssertionScoreDirectorFactory() != null) {
                throw new IllegalArgumentException(
                        "A assertionScoreDirectorFactory (%s) cannot have a non-null assertionScoreDirectorFactory (%s)."
                                .formatted(assertionScoreDirectorFactoryConfig,
                                        assertionScoreDirectorFactoryConfig.getAssertionScoreDirectorFactory()));
            }
            if (environmentMode.compareTo(EnvironmentMode.STEP_ASSERT) > 0) {
                throw new IllegalArgumentException(
                        "A non-null assertionScoreDirectorFactory (%s) requires an environmentMode (%s) of %s or lower."
                                .formatted(assertionScoreDirectorFactoryConfig, environmentMode, EnvironmentMode.STEP_ASSERT));
            }
            // Built from its own config, which has no phases, so this is always a concrete factory:
            // the code reading an assertion factory expects one it can use directly.
            var assertScoreDirectorFactory =
                    new ScoreDirectorFactoryFactory<Solution_, Score_>(assertionScoreDirectorFactoryConfig);
            factory.setAssertionScoreDirectorFactory(
                    assertScoreDirectorFactory.buildScoreDirectorFactory(EnvironmentMode.NON_REPRODUCIBLE, solutionDescriptor));
        }
        factory.setInitializingScoreTrend(decideInitializingScoreTrend(scoreDirectorFactoryConfig, solutionDescriptor));
        return factory;
    }

    private static <Solution_> InitializingScoreTrend decideInitializingScoreTrend(ScoreDirectorFactoryConfig config,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var initializingScoreTrend = config.getInitializingScoreTrend() == null ? InitializingScoreTrendLevel.ANY.name()
                : config.getInitializingScoreTrend();
        return InitializingScoreTrend.parseTrend(initializingScoreTrend,
                solutionDescriptor.getScoreDefinition().getLevelsSize());
    }

    /**
     * Picks the one implementation the config selected; {@link #assertCorrectDirectorFactory} has already
     * rejected a config selecting more than one.
     */
    private AbstractScoreDirectorFactory<Solution_, Score_, ?> decideScoreDirectorFactory(
            SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentMode) {
        if (scoreDirectorFactoryConfig.getEasyScoreCalculatorClass() != null) {
            return EasyScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor, scoreDirectorFactoryConfig,
                    environmentMode);
        } else if (scoreDirectorFactoryConfig.getIncrementalScoreCalculatorClass() != null) {
            return IncrementalScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor, scoreDirectorFactoryConfig,
                    environmentMode);
        } else if (scoreDirectorFactoryConfig.getConstraintProviderClass() != null) {
            return BavetConstraintStreamScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor,
                    scoreDirectorFactoryConfig,
                    environmentMode);
        } else {
            throw new IllegalArgumentException(
                    "The scoreDirectorFactory lacks configuration for either constraintProviderClass, " +
                            "easyScoreCalculatorClass or incrementalScoreCalculatorClass.");
        }
    }

    /**
     * Whether the score directors built for the given environment mode have to track constraint matches,
     * which carries a performance penalty.
     *
     * @param environmentMode the environment mode the score director will run in,
     *        which for a phase running in its own mode is that phase's
     */
    public static <Solution_> ConstraintMatchPolicy decideConstraintMatchPolicy(SolverScope<Solution_> solverScope,
            EnvironmentMode environmentMode) {
        return solverScope.isAnyMetricConstraintMatchBased() || environmentMode.isStepAssertOrMore()
                ? ConstraintMatchPolicy.ENABLED
                : ConstraintMatchPolicy.DISABLED;
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
        if (config.getConstraintStreamProfilingEnabled() != null
                && config.getConstraintStreamProfilingEnabled()
                && !hasConstraintProvider) {
            throw new IllegalStateException(
                    "If there is no constraintProviderClass (%s), then constraintStreamProfilingEnabled (%s) must be false."
                            .formatted(constraintProviderClass, config.getConstraintStreamProfilingEnabled()));
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
