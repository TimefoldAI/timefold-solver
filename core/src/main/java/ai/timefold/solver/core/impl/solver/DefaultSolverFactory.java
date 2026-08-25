package ai.timefold.solver.core.impl.solver;

import java.time.Clock;
import java.util.ArrayList;
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
 * Builds {@link DefaultSolver} instances out of a {@link SolverConfig},
 * and owns the state which is expensive to build and therefore shared by every solver it builds:
 * the {@link SolutionDescriptor} and a single {@link DelegateScoreDirectorFactory}.
 * <p>
 * The solver config has one environment mode, the global one,
 * and each of its phases may override it with a stricter one.
 * The score director factory is built once for the global environment mode;
 * a phase which overrides the mode asks the delegate for a score director in its own mode instead,
 * which the delegate serves without this factory having to keep one instance per mode.
 * <p>
 * That is also why a global environment mode has to exist at all,
 * even for a config whose phases all override it.
 * Some components depend on the score director factory
 * while being decoupled from the solving life cycle,
 * and therefore have no phase whose environment mode they could adopt;
 * {@link SolverManager} and the integrations
 * ({@code TimefoldSolverBeanFactory} injecting a {@link ConstraintMetaModel}, for instance)
 * are such components.
 * They all get the global environment mode.
 * <p>
 * {@link #assertEnvironmentModeConfiguration(SolverConfig)} guards the invariants this relies on:
 * no phase may be less strict than the global mode,
 * at least one phase must actually use the global mode,
 * and a non-reproducible global mode admits no phase-level override at all.
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
    private final EnvironmentMode globalEnvironmentMode;
    private final DelegateScoreDirectorFactory<Solution_, ?> delegateScoreDirectorFactory;
    private final DomainAccessType domainAccessType;

    public DefaultSolverFactory(SolverConfig solverConfig) {
        this(solverConfig, DomainAccessType.AUTO);
    }

    public DefaultSolverFactory(SolverConfig solverConfig, DomainAccessType domainAccessType) {
        this.domainAccessType = domainAccessType;
        this.clock = Objects.requireNonNullElse(solverConfig.getClock(), Clock.systemDefaultZone());
        this.solverConfig =
                Objects.requireNonNull(solverConfig, "The solverConfig (%s) cannot be null.".formatted(solverConfig));
        this.globalEnvironmentMode = assertEnvironmentModeConfiguration(solverConfig);
        this.solutionDescriptor = buildSolutionDescriptor();
        // Caching score director factory for the default environment mode as it potentially does expensive things
        this.delegateScoreDirectorFactory =
                new DelegateScoreDirectorFactory<>(solverConfig, solutionDescriptor, globalEnvironmentMode);
    }

    public Clock getClock() {
        return clock;
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    /**
     * @return the factory built for the default environment mode;
     *         the delegate and not its {@link DelegateScoreDirectorFactory} wrapper,
     *         as callers outside the solving life cycle expect the concrete implementation,
     *         such as {@code BeanUtil#buildConstraintMetaModel} which needs a constraint stream factory
     */
    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> ScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory() {
        return (ScoreDirectorFactory<Solution_, Score_>) delegateScoreDirectorFactory.getDelegate();
    }

    @Override
    public Solver<Solution_> buildSolver(SolverConfigOverride configOverride) {
        Objects.requireNonNull(configOverride, "Invalid configOverride (null) given to SolverFactory.");
        var isDaemon = Objects.requireNonNullElse(solverConfig.getDaemon(), false);

        var solverScope = new SolverScope<Solution_>(clock);
        var monitoringConfig = solverConfig.determineMetricConfig();
        solverScope.setMonitoringTags(Tags.empty());
        var solverMetricList = Objects.requireNonNull(monitoringConfig.getSolverMetricList());
        if (!solverMetricList.isEmpty()) {
            solverScope.setSolverMetricSet(EnumSet.copyOf(solverMetricList));
        } else {
            solverScope.setSolverMetricSet(EnumSet.noneOf(SolverMetric.class));
        }
        var scoreDirector = delegateScoreDirectorFactory.createScoreDirectorBuilder(globalEnvironmentMode)
                .withLookUpEnabled(true) // Custom phases and problem changes may rely on lookups.
                .withConstraintMatchPolicy(delegateScoreDirectorFactory.decideConstraintMatchPolicy(globalEnvironmentMode))
                .build();
        solverScope.setScoreDirector(scoreDirector);
        solverScope.setProblemChangeDirector(new DefaultProblemChangeDirector<>(scoreDirector));
        var moveThreadCount = resolveMoveThreadCount(true);
        var bestSolutionRecaller =
                BestSolutionRecallerFactory.create().<Solution_> buildBestSolutionRecaller(globalEnvironmentMode);
        var randomFactory = buildRandomSupplier(globalEnvironmentMode);
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
                .withEnvironmentMode(globalEnvironmentMode)
                .withMoveThreadCount(moveThreadCount)
                .withMoveThreadBufferSize(solverConfig.getMoveThreadBufferSize())
                .withThreadFactoryClass(solverConfig.getThreadFactoryClass())
                .withNearbyDistanceMeterClass(solverConfig.getNearbyDistanceMeterClass())
                .withRandom(randomFactory.get())
                .withInitializingScoreTrend(delegateScoreDirectorFactory.getInitializingScoreTrend())
                .withSolutionDescriptor(solutionDescriptor)
                .withClassInstanceCache(ClassInstanceCache.create())
                .build();
        var basicPlumbingTermination = new BasicPlumbingTermination<Solution_>(isDaemon);
        var termination = buildTermination(basicPlumbingTermination, configPolicy, configOverride);
        var phaseList = buildPhaseList(configPolicy, bestSolutionRecaller, termination);

        return new DefaultSolver<>(globalEnvironmentMode, delegateScoreDirectorFactory, randomFactory, bestSolutionRecaller,
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
        var globalEnvironmentMode = solverConfig.determineEnvironmentMode();
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (ConfigUtils.isEmptyCollection(phaseConfigList)) {
            return globalEnvironmentMode;
        }
        var phaseEnvironmentList =
                phaseConfigList.stream()
                        .map(phaseConfig -> Objects.requireNonNullElse(phaseConfig.getEnvironmentMode(),
                                globalEnvironmentMode))
                        .toList();
        if (globalEnvironmentMode == EnvironmentMode.NON_REPRODUCIBLE
                && phaseEnvironmentList.stream().anyMatch(environmentMode -> environmentMode != globalEnvironmentMode)) {
            // A non-reproducible global environment mode cannot be overridden per phase,
            // as a phase-level override would have nothing reproducible to be an override of.
            throw new IllegalStateException(
                    "Phase-level environmentMode override is only possible when global environmentMode is reproducible, but was %s."
                            .formatted(globalEnvironmentMode.name()));
        }
        // If none of the phase environments use the global environment, we fail fast.
        var checkGlobalEnvironment = phaseEnvironmentList.isEmpty();
        for (var phaseEnvironment : phaseEnvironmentList) {
            if (phaseEnvironment == globalEnvironmentMode) {
                checkGlobalEnvironment = true;
                break;
            }
        }
        if (!checkGlobalEnvironment) {
            throw new IllegalStateException("""
                    The global environment mode is %s, but none of the phase environment modes are using it [%s].
                    Maybe adjust at least one of the phase environment modes to match the global environmentMode (%s)"""
                    .formatted(
                            globalEnvironmentMode.name(),
                            String.join(", ", phaseEnvironmentList.stream().map(EnvironmentMode::name).toList()),
                            globalEnvironmentMode.name()));
        }
        var invalidPhaseEnvironmentList = new ArrayList<String>(phaseConfigList.size());
        for (var phaseEnvironment : phaseEnvironmentList) {
            if (phaseEnvironment.ordinal() > globalEnvironmentMode.ordinal()) {
                invalidPhaseEnvironmentList.add(phaseEnvironment.name());
            }
        }
        if (!invalidPhaseEnvironmentList.isEmpty()) {
            // The phase environments must have an assertion level greater than or equal to the global environment level
            throw new IllegalStateException(
                    "The phase environments must have an assertion level higher than or equal to the global environment level (%s). The following phase environment modes are not valid: [%s]."
                            .formatted(globalEnvironmentMode.name(), String.join(", ", invalidPhaseEnvironmentList)));
        }
        return globalEnvironmentMode;
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
