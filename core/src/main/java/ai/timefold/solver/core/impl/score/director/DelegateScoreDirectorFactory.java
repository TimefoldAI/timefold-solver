package ai.timefold.solver.core.impl.score.director;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector.AbstractScoreDirectorBuilder;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.incremental.IncrementalScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The single entry point for turning a {@link ScoreDirectorFactoryConfig} into score directors.
 * It decides which score calculation implementation the configuration selected
 * ({@link EasyScoreDirectorFactory}, {@link IncrementalScoreDirectorFactory}
 * or {@link BavetConstraintStreamScoreDirectorFactory})
 * and delegates to a factory of that type, hiding the choice from its callers.
 * <p>
 * Building a delegate is potentially expensive,
 * therefore exactly one is built eagerly for the default environment mode
 * and then reused for every score director requested for that mode.
 * <p>
 * Since a solver phase may override the solver's environment mode,
 * {@link #createScoreDirectorBuilder(EnvironmentMode)} may be called with a different mode than the default one.
 * Most delegates only pass the environment mode on to the score director they build,
 * so they can serve any mode and are reused as they are.
 * The exception is {@link BavetConstraintStreamScoreDirectorFactory},
 * which builds its constraint network from the environment mode up front;
 * for it, a separate delegate is built for the requested mode,
 * then cached and shared like the default one, as building it is expensive.
 * <p>
 * On top of picking the delegate, this factory applies the parts of the configuration
 * which are shared by all implementations:
 * the {@link InitializingScoreTrend},
 * the optional assertion score director factory,
 * and the {@link ConstraintMatchPolicy} implied by the environment mode and the enabled metrics.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution}
 *        annotation
 * @param <Score_> the score type to go with the solution
 */
@NullMarked
public class DelegateScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        implements ScoreDirectorFactory<Solution_, Score_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegateScoreDirectorFactory.class);
    private final ScoreDirectorFactoryConfig config;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final EnvironmentMode globalEnvironmentMode;
    private final ScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory;
    private final List<SolverMetric> metricsRequiringConstraintMatchList;
    private final boolean requireNewFactoryOnDifferentEnvironment;
    /**
     * Only holds modes other than the global one;
     * the global mode's factory is {@link #scoreDirectorFactory}.
     */
    private final Map<EnvironmentMode, ScoreDirectorFactory<Solution_, Score_>> environmentModeToFactoryMap =
            new ConcurrentHashMap<>();

    /**
     * The constructor used by the solver,
     * as only a full {@link SolverConfig} tells which metrics require constraint matching.
     *
     * @param environmentMode the default environment mode, typically the solver's
     */
    public DelegateScoreDirectorFactory(SolverConfig solverConfig, SolutionDescriptor<Solution_> solutionDescriptor,
            EnvironmentMode environmentMode) {
        this(Objects.requireNonNullElseGet(solverConfig.getScoreDirectorFactoryConfig(), ScoreDirectorFactoryConfig::new),
                solutionDescriptor, environmentMode, determineMetricsRequiringConstraintMatch(solverConfig));
    }

    /**
     * As defined by the constructor which also takes a metric list,
     * with no metrics requiring constraint matching.
     */
    public DelegateScoreDirectorFactory(ScoreDirectorFactoryConfig config, SolutionDescriptor<Solution_> solutionDescriptor,
            EnvironmentMode environmentMode) {
        this(config, solutionDescriptor, environmentMode, Collections.emptyList());
    }

    /**
     * @param config the score factory configuration
     * @param solutionDescriptor the solution descriptor
     * @param environmentMode the default environment mode;
     *        the delegate is built eagerly for this mode, as building it is potentially expensive
     * @param metricsRequiringConstraintMatchList the enabled metrics which can only be computed
     *        when the score directors track constraint matches
     */
    public DelegateScoreDirectorFactory(ScoreDirectorFactoryConfig config, SolutionDescriptor<Solution_> solutionDescriptor,
            EnvironmentMode environmentMode, List<SolverMetric> metricsRequiringConstraintMatchList) {
        this.config = Objects.requireNonNull(config);
        assertCorrectDirectorFactory(config);
        this.solutionDescriptor = solutionDescriptor;
        this.globalEnvironmentMode = environmentMode;
        this.metricsRequiringConstraintMatchList = metricsRequiringConstraintMatchList;
        this.scoreDirectorFactory = internalBuildScoreDirectorFactory(solutionDescriptor, environmentMode);
        // Constraint Stream factory requires a new factory if the environment changes
        this.requireNewFactoryOnDifferentEnvironment = config.getConstraintProviderClass() != null;
        if (!metricsRequiringConstraintMatchList.isEmpty() && !environmentMode.isStepAssertOrMore()) {
            LOGGER.info(
                    "Enabling constraint matching as required by the enabled metrics ({}). This will impact solver performance.",
                    metricsRequiringConstraintMatchList);
        }
    }

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    @Override
    public ScoreDefinition<Score_> getScoreDefinition() {
        return scoreDirectorFactory.getScoreDefinition();
    }

    @Override
    public @Nullable InitializingScoreTrend getInitializingScoreTrend() {
        return scoreDirectorFactory.getInitializingScoreTrend();
    }

    /**
     * Exposes the factory built for the default environment mode,
     * for the benefit of the code which needs the concrete implementation rather than this wrapper,
     * such as the Quarkus and Spring integrations building a
     * {@link ConstraintMetaModel}.
     * Prefer this factory itself wherever the environment mode may still vary.
     */
    public ScoreDirectorFactory<Solution_, Score_> getDelegate() {
        return scoreDirectorFactory;
    }

    @Override
    public AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder() {
        return createScoreDirectorBuilder(globalEnvironmentMode);
    }

    @Override
    public AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder(EnvironmentMode environmentMode) {
        if (environmentMode != globalEnvironmentMode && requireNewFactoryOnDifferentEnvironment) {
            // The BavetConstraintStreamScoreDirectorFactory creates a BavetConstraintFactory based on the environment
            // and requires a new factory to generate a new score director with a different environment.
            // Building one is expensive, so each mode gets its factory built at most once and then shared,
            // exactly as the global mode's factory is shared.
            var factory = environmentModeToFactoryMap.computeIfAbsent(environmentMode,
                    mode -> internalBuildScoreDirectorFactory(solutionDescriptor, mode));
            return factory.createScoreDirectorBuilder();
        } else {
            return scoreDirectorFactory.createScoreDirectorBuilder(environmentMode);
        }
    }

    private ScoreDirectorFactory<Solution_, Score_> internalBuildScoreDirectorFactory(
            SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentMode) {
        var factory = decideMultipleScoreDirectorFactories(solutionDescriptor, environmentMode);
        var assertionScoreDirectorFactoryConfig = config.getAssertionScoreDirectorFactory();
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
            var assertScoreDirectorFactory =
                    new DelegateScoreDirectorFactory<Solution_, Score_>(assertionScoreDirectorFactoryConfig, solutionDescriptor,
                            EnvironmentMode.NON_REPRODUCIBLE, Collections.emptyList());
            // We use the delegate to prevent issues in areas where non-delegate factories are expected
            factory.setAssertionScoreDirectorFactory(assertScoreDirectorFactory.getDelegate());
        }
        factory.setInitializingScoreTrend(decideInitializingScoreTrend(config, solutionDescriptor));
        return factory;
    }

    private static List<SolverMetric> determineMetricsRequiringConstraintMatch(SolverConfig solverConfig) {
        var monitoringConfig = solverConfig.determineMetricConfig();
        var solverMetricList = Objects.requireNonNull(monitoringConfig.getSolverMetricList());
        return solverMetricList.stream()
                .filter(SolverMetric::isMetricConstraintMatchBased)
                .toList();
    }

    private static <Solution_> InitializingScoreTrend decideInitializingScoreTrend(ScoreDirectorFactoryConfig config,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var initializingScoreTrend = config.getInitializingScoreTrend() == null ? InitializingScoreTrendLevel.ANY.name()
                : config.getInitializingScoreTrend();
        return InitializingScoreTrend.parseTrend(initializingScoreTrend,
                solutionDescriptor.getScoreDefinition().getLevelsSize());
    }

    /**
     * Unlike the default implementation,
     * this also enables constraint matching when a metric requires it.
     */
    @Override
    public ConstraintMatchPolicy decideConstraintMatchPolicy(EnvironmentMode environmentMode) {
        return !metricsRequiringConstraintMatchList.isEmpty() || environmentMode.isStepAssertOrMore()
                ? ConstraintMatchPolicy.ENABLED
                : ConstraintMatchPolicy.DISABLED;
    }

    private AbstractScoreDirectorFactory<Solution_, Score_, ?> decideMultipleScoreDirectorFactories(
            SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentMode) {
        // At this point, we're guaranteed to have at most one score director factory selected.
        if (config.getEasyScoreCalculatorClass() != null) {
            return EasyScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor, config, environmentMode);
        } else if (config.getIncrementalScoreCalculatorClass() != null) {
            return IncrementalScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor, config, environmentMode);
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
