package ai.timefold.solver.core.impl.solver;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.AbstractFromConfigFactory;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.domain.common.DomainAccessType;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.PhaseFactory;
import ai.timefold.solver.core.impl.score.director.DelegateScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.random.DefaultRandomSource;
import ai.timefold.solver.core.impl.solver.random.RandomSource;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecallerFactory;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;
import ai.timefold.solver.core.impl.solver.termination.UniversalTermination;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Tags;

/**
 * The default solver factory must maintain a default score director factory,
 * as some solver components depend on this factory,
 * including {@link SolverManager} and {@code TimefoldSolverBeanFactory}.
 * <p>
 * The proposed approach establishes that the configuration defines a root environment mode,
 * which is used to create the default score director factory.
 * Since the phases can override the environment,
 * the delegate factory will enable the creation of separate factories
 * while maintaining a default one that is used for all other components.
 * <p>
 * The necessity for a default environment mode can be illustrated by the following use case.
 * Imagine a configuration that includes multiple phases, each with a different environment mode.
 * If a Quarkus application needs to inject a {@link ConstraintMetaModel}
 * instance, this instance depends on the score director factory,
 * which in turn relies on the environment mode.
 * If multiple phase environments exist,
 * selecting one of these environments is not possible
 * as this injection point is decoupled from the solving life cycle.
 * 
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see SolverFactory
 */
@NullMarked
public final class DefaultSolverFactory<Solution_> implements SolverFactory<Solution_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSolverFactory.class);
    private static final long DEFAULT_RANDOM_SEED = 0L;

    private final Clock clock;
    private final SolverConfig solverConfig;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final EnvironmentMode defaultEnvironmentMode;
    private final DelegateScoreDirectorFactory<Solution_, ?> delegateScoreDirectorFactory;
    private final ScoreDirectorFactory<Solution_, ?> defaultScoreDirectorFactory;
    private final DomainAccessType domainAccessType;

    public DefaultSolverFactory(SolverConfig solverConfig) {
        this(solverConfig, DomainAccessType.AUTO);
    }

    public DefaultSolverFactory(SolverConfig solverConfig, DomainAccessType domainAccessType) {
        this.domainAccessType = domainAccessType;
        this.clock = Objects.requireNonNullElse(solverConfig.getClock(), Clock.systemDefaultZone());
        this.solverConfig =
                Objects.requireNonNull(solverConfig, "The solverConfig (%s) cannot be null.".formatted(solverConfig));
        this.defaultEnvironmentMode = assertEnvironmentModeConfiguration(solverConfig);
        this.solutionDescriptor = buildSolutionDescriptor();
        var scoreDirectorFactoryConfig =
                Objects.requireNonNullElseGet(solverConfig.getScoreDirectorFactoryConfig(), ScoreDirectorFactoryConfig::new);
        var hasMetricRequiringConstraintMatch = hasMetricRequiringConstraintMatch(solverConfig);
        this.delegateScoreDirectorFactory = new DelegateScoreDirectorFactory<>(
                Objects.requireNonNull(scoreDirectorFactoryConfig), hasMetricRequiringConstraintMatch);
        // Caching score director factory as it potentially does expensive things
        this.defaultScoreDirectorFactory =
                this.delegateScoreDirectorFactory.buildScoreDirectorFactory(defaultEnvironmentMode, solutionDescriptor);
    }

    private static boolean hasMetricRequiringConstraintMatch(SolverConfig solverConfig) {
        var monitoringConfig = solverConfig.determineMetricConfig();
        var solverMetricList = Objects.requireNonNull(monitoringConfig.getSolverMetricList());
        var metricsRequiringConstraintMatch = false;
        if (!solverMetricList.isEmpty()) {
            metricsRequiringConstraintMatch = !solverMetricList.stream()
                    .filter(SolverMetric::isMetricConstraintMatchBased)
                    .toList()
                    .isEmpty();
        }
        return metricsRequiringConstraintMatch;
    }

    public Clock getClock() {
        return clock;
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> ScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory() {
        return (ScoreDirectorFactory<Solution_, Score_>) defaultScoreDirectorFactory;
    }

    @Override
    public Solver<Solution_> buildSolver(SolverConfigOverride configOverride) {
        Objects.requireNonNull(configOverride, "Invalid configOverride (null) given to SolverFactory.");
        var isDaemon = Objects.requireNonNullElse(solverConfig.getDaemon(), false);

        var solverScope = new SolverScope<Solution_>(clock);
        var monitoringConfig = solverConfig.determineMetricConfig();
        solverScope.setMonitoringTags(Tags.empty());
        var solverMetricList = Objects.requireNonNull(monitoringConfig.getSolverMetricList());
        var metricsRequiringConstraintMatchSet = Collections.<SolverMetric> emptyList();
        if (!solverMetricList.isEmpty()) {
            solverScope.setSolverMetricSet(EnumSet.copyOf(solverMetricList));
            metricsRequiringConstraintMatchSet = solverScope.getSolverMetricSet().stream()
                    .filter(SolverMetric::isMetricConstraintMatchBased)
                    .filter(solverScope::isMetricEnabled)
                    .toList();
        } else {
            solverScope.setSolverMetricSet(EnumSet.noneOf(SolverMetric.class));
        }
        var isStepAssertOrMore = defaultEnvironmentMode.isStepAssertOrMore();
        var constraintMatchEnabled = !metricsRequiringConstraintMatchSet.isEmpty() || isStepAssertOrMore;
        if (constraintMatchEnabled && !isStepAssertOrMore) {
            LOGGER.info(
                    "Enabling constraint matching as required by the enabled metrics ({}). This will impact solver performance.",
                    metricsRequiringConstraintMatchSet);
        }
        var scoreDirector = delegateScoreDirectorFactory.createScoreDirector(getScoreDirectorFactory());
        solverScope.setScoreDirector(scoreDirector);
        solverScope.setProblemChangeDirector(new DefaultProblemChangeDirector<>(scoreDirector));
        var moveThreadCount = resolveMoveThreadCount(true);
        var bestSolutionRecaller =
                BestSolutionRecallerFactory.create().<Solution_> buildBestSolutionRecaller(defaultEnvironmentMode);
        var randomFactory = buildRandomSupplier(defaultEnvironmentMode);
        var previewFeaturesEnabled = solverConfig.getEnablePreviewFeatureSet();

        var scoreDirectorFactoryConfig = solverConfig.getScoreDirectorFactoryConfig();
        if (scoreDirectorFactoryConfig != null) {
            var profilingEnabled = scoreDirectorFactoryConfig.getConstraintStreamProfilingEnabled();
            if (moveThreadCount != null && profilingEnabled != null && profilingEnabled) {
                throw new UnsupportedOperationException(
                        "Multithreaded solving is not supported together with constraintStreamProfilingEnabled (%s)."
                                .formatted(profilingEnabled));
            }
        }

        var configPolicy = new HeuristicConfigPolicy.Builder<Solution_>()
                .withPreviewFeatureSet(previewFeaturesEnabled)
                .withEnvironmentMode(defaultEnvironmentMode)
                .withMoveThreadCount(moveThreadCount)
                .withMoveThreadBufferSize(solverConfig.getMoveThreadBufferSize())
                .withThreadFactoryClass(solverConfig.getThreadFactoryClass())
                .withNearbyDistanceMeterClass(solverConfig.getNearbyDistanceMeterClass())
                .withRandom(randomFactory.get())
                .withInitializingScoreTrend(defaultScoreDirectorFactory.getInitializingScoreTrend())
                .withSolutionDescriptor(solutionDescriptor)
                .withClassInstanceCache(ClassInstanceCache.create())
                .build();
        var basicPlumbingTermination = new BasicPlumbingTermination<Solution_>(isDaemon);
        var termination = buildTermination(basicPlumbingTermination, configPolicy, configOverride);
        var phaseList = buildPhaseList(configPolicy, bestSolutionRecaller, termination);

        return new DefaultSolver<>(defaultEnvironmentMode, delegateScoreDirectorFactory, randomFactory, bestSolutionRecaller,
                basicPlumbingTermination, (UniversalTermination<Solution_>) termination, phaseList, solverScope,
                moveThreadCount == null ? SolverConfig.MOVE_THREAD_COUNT_NONE : Integer.toString(moveThreadCount));
    }

    public @Nullable Integer resolveMoveThreadCount(boolean enforceMaximum) {
        var maybeCount =
                new MoveThreadCountResolver().resolveMoveThreadCount(solverConfig.getMoveThreadCount(), enforceMaximum);
        if (maybeCount.isPresent()) {
            return maybeCount.getAsInt();
        } else {
            return null;
        }
    }

    private SolverTermination<Solution_> buildTermination(BasicPlumbingTermination<Solution_> basicPlumbingTermination,
            HeuristicConfigPolicy<Solution_> configPolicy, SolverConfigOverride solverConfigOverride) {
        var terminationConfig = Objects.requireNonNullElseGet(solverConfigOverride.getTerminationConfig(),
                () -> Objects.requireNonNullElseGet(solverConfig.getTerminationConfig(), TerminationConfig::new));
        return TerminationFactory.<Solution_> create(Objects.requireNonNull(terminationConfig))
                .buildTermination(configPolicy, basicPlumbingTermination);
    }

    private SolutionDescriptor<Solution_> buildSolutionDescriptor() {
        if (solverConfig.getSolutionClass() == null) {
            throw new IllegalArgumentException(
                    "The solver configuration must have a solutionClass (%s). If you're using the Quarkus extension or Spring Boot starter, it should have been filled in already."
                            .formatted(solverConfig.getSolutionClass()));
        }
        if (ConfigUtils.isEmptyCollection(solverConfig.getEntityClassList())) {
            throw new IllegalArgumentException(
                    "The solver configuration must have at least 1 entityClass (%s). If you're using the Quarkus extension or Spring Boot starter, it should have been filled in already."
                            .formatted(solverConfig.getEntityClassList()));
        }
        return SolutionDescriptor.buildSolutionDescriptor(solverConfig.getEnablePreviewFeatureSet(),
                domainAccessType,
                (Class<Solution_>) solverConfig.getSolutionClass(),
                solverConfig.getGizmoMemberAccessorMap(),
                solverConfig.getGizmoSolutionClonerMap(),
                solverConfig.getEntityClassList());
    }

    Supplier<RandomSource> buildRandomSupplier(EnvironmentMode environmentMode) {
        var randomSeed = solverConfig.getRandomSeed();
        if (randomSeed == null && environmentMode != EnvironmentMode.NON_REPRODUCIBLE) {
            randomSeed = DEFAULT_RANDOM_SEED;
        } else if (randomSeed == null) {
            randomSeed = RandomGenerator.getDefault().nextLong();
        }
        return DefaultRandomSource.seededSupplier(randomSeed);
    }

    public List<Phase<Solution_>> buildPhaseList(HeuristicConfigPolicy<Solution_> configPolicy,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, SolverTermination<Solution_> termination) {
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (ConfigUtils.isEmptyCollection(phaseConfigList)) {
            var genuineEntityDescriptorCollection = configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors();
            phaseConfigList = new ArrayList<>(genuineEntityDescriptorCollection.size() + 2);
            for (var entityDescriptor : genuineEntityDescriptorCollection) {
                if (entityDescriptor.hasBothListAndBasicVariables()) {
                    // We add a separate step for each variable type
                    phaseConfigList.add(buildConstructionHeuristicPhaseConfigForBasicVariable(configPolicy, entityDescriptor));
                    phaseConfigList.add(buildConstructionHeuristicPhaseConfigForListVariable(configPolicy, entityDescriptor));
                } else if (entityDescriptor.hasAnyListVariables()) {
                    // There is no need to revalidate the number of list variables,
                    // as it has already been validated in SolutionDescriptor
                    phaseConfigList.add(buildConstructionHeuristicPhaseConfigForListVariable(configPolicy, entityDescriptor));
                } else {
                    phaseConfigList.add(buildConstructionHeuristicPhaseConfigForBasicVariable(configPolicy, entityDescriptor));
                }
            }
            phaseConfigList.add(new LocalSearchPhaseConfig());
        }
        return PhaseFactory.buildPhases(phaseConfigList, configPolicy, bestSolutionRecaller, termination);
    }

    private ConstructionHeuristicPhaseConfig
            buildConstructionHeuristicPhaseConfigForBasicVariable(HeuristicConfigPolicy<Solution_> configPolicy,
                    EntityDescriptor<Solution_> entityDescriptor) {
        var constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        constructionHeuristicPhaseConfig
                .setEntityPlacerConfig(new QueuedEntityPlacerConfig().withEntitySelectorConfig(AbstractFromConfigFactory
                        .getDefaultEntitySelectorConfigForEntity(configPolicy, entityDescriptor)));
        return constructionHeuristicPhaseConfig;
    }

    private ConstructionHeuristicPhaseConfig
            buildConstructionHeuristicPhaseConfigForListVariable(HeuristicConfigPolicy<Solution_> configPolicy,
                    EntityDescriptor<Solution_> entityDescriptor) {
        var constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        var listVariableDescriptor = entityDescriptor.getListVariableDescriptor();
        constructionHeuristicPhaseConfig
                .setEntityPlacerConfig(DefaultConstructionHeuristicPhaseFactory
                        .buildListVariableQueuedValuePlacerConfig(configPolicy, listVariableDescriptor));
        return constructionHeuristicPhaseConfig;
    }

    public void ensurePreviewFeature(PreviewFeature previewFeature) {
        HeuristicConfigPolicy.ensurePreviewFeature(previewFeature, solverConfig.getEnablePreviewFeatureSet());
    }

    private static EnvironmentMode assertEnvironmentModeConfiguration(SolverConfig solverConfig) {
        var defaultEnvironmentMode = solverConfig.determineEnvironmentMode();
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (ConfigUtils.isEmptyCollection(phaseConfigList)) {
            return defaultEnvironmentMode;
        }
        var phaseEnvironmentList =
                phaseConfigList.stream()
                        .map(phaseConfig -> Objects.requireNonNullElse(phaseConfig.getEnvironmentMode(),
                                defaultEnvironmentMode))
                        .toList();
        if (defaultEnvironmentMode == EnvironmentMode.NON_REPRODUCIBLE
                && phaseEnvironmentList.stream().anyMatch(environmentMode -> environmentMode != defaultEnvironmentMode)) {
            // If the default environment is non-reproducible,
            // then all phase environment modes must also be non-reproducible
            throw new IllegalStateException(
                    "The default environment mode is (%s), and all phase environments [%s] must also be non-reproducible."
                            .formatted(defaultEnvironmentMode.name(),
                                    String.join(", ", phaseEnvironmentList.stream().map(EnvironmentMode::name).toList())));
        }
        // If none of the phase environments use the default environment, we fail fast.
        var checkDefaultEnvironment = phaseEnvironmentList.isEmpty();
        for (var phaseEnvironment : phaseEnvironmentList) {
            if (phaseEnvironment == defaultEnvironmentMode) {
                checkDefaultEnvironment = true;
                break;
            }
        }
        if (!checkDefaultEnvironment) {
            throw new IllegalStateException("""
                    The default environment mode (%s) is not used in any of the defined phases environment modes [%s].
                    Maybe adjust the solver config's default environment mode.
                    Maybe adjust at least one of the phase environment modes to match the default environment mode (%s)"""
                    .formatted(
                            defaultEnvironmentMode.name(),
                            String.join(", ", phaseEnvironmentList.stream().map(EnvironmentMode::name).toList()),
                            defaultEnvironmentMode.name()));
        }
        var invalidPhaseEnvironmentList = new ArrayList<String>(phaseConfigList.size());
        for (var phaseEnvironment : phaseEnvironmentList) {
            if (phaseEnvironment.ordinal() > defaultEnvironmentMode.ordinal()) {
                invalidPhaseEnvironmentList.add(phaseEnvironment.name());
            }
        }
        if (!invalidPhaseEnvironmentList.isEmpty()) {
            // The phase environments must have an assertion level greater than or equal to the default environment level
            throw new IllegalStateException(
                    "The phase environments must have an assertion level higher than or equal to the default environment level (%s). The following phase environment modes are not valid: [%s]."
                            .formatted(defaultEnvironmentMode.name(), String.join(", ", invalidPhaseEnvironmentList)));
        }
        return defaultEnvironmentMode;
    }

    // Required for testability as final classes cannot be mocked.
    static class MoveThreadCountResolver {

        protected OptionalInt resolveMoveThreadCount(String moveThreadCount) {
            return resolveMoveThreadCount(moveThreadCount, true);
        }

        protected OptionalInt resolveMoveThreadCount(@Nullable String moveThreadCount, boolean enforceMaximum) {
            var availableProcessorCount = getAvailableProcessors();
            int resolvedMoveThreadCount;
            if (moveThreadCount == null || moveThreadCount.equals(SolverConfig.MOVE_THREAD_COUNT_NONE)) {
                return OptionalInt.empty();
            } else if (moveThreadCount.equals(SolverConfig.MOVE_THREAD_COUNT_AUTO)) {
                // Leave one for the Operating System and 1 for the solver thread, take the rest
                resolvedMoveThreadCount = (availableProcessorCount - 2);
                if (enforceMaximum && resolvedMoveThreadCount > 4) {
                    // A moveThreadCount beyond 4 is currently typically slower
                    resolvedMoveThreadCount = 4;
                }
                if (resolvedMoveThreadCount <= 1) {
                    // Fall back to single threaded solving with no move threads.
                    // To deliberately enforce 1 moveThread, set the moveThreadCount explicitly to 1.
                    return OptionalInt.empty();
                }
            } else {
                resolvedMoveThreadCount = ConfigUtils.resolvePoolSize("moveThreadCount", moveThreadCount,
                        SolverConfig.MOVE_THREAD_COUNT_NONE, SolverConfig.MOVE_THREAD_COUNT_AUTO);
            }
            if (resolvedMoveThreadCount < 1) {
                throw new IllegalArgumentException(
                        "The moveThreadCount (%s) resulted in a resolvedMoveThreadCount (%d) that is lower than 1."
                                .formatted(moveThreadCount, resolvedMoveThreadCount));
            }
            if (resolvedMoveThreadCount > availableProcessorCount) {
                LOGGER.warn(
                        "The resolvedMoveThreadCount ({}) is higher than the availableProcessorCount ({}), which is counter-efficient.",
                        resolvedMoveThreadCount, availableProcessorCount);
                // Still allow it, to reproduce issues of a high-end server machine on a low-end developer machine
            }
            return OptionalInt.of(resolvedMoveThreadCount);
        }

        protected int getAvailableProcessors() {
            return Runtime.getRuntime().availableProcessors();
        }
    }
}
