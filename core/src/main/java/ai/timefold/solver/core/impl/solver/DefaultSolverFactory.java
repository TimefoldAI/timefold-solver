package ai.timefold.solver.core.impl.solver;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.random.RandomType;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.AbstractFromConfigFactory;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.PhaseFactory;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.random.DefaultRandomFactory;
import ai.timefold.solver.core.impl.solver.random.RandomFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecallerFactory;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;
import ai.timefold.solver.core.impl.solver.termination.UniversalTermination;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Tags;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see SolverFactory
 */
public final class DefaultSolverFactory<Solution_> implements SolverFactory<Solution_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSolverFactory.class);
    private static final long DEFAULT_RANDOM_SEED = 0L;

    private final Clock clock;
    private final SolverConfig solverConfig;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final ScoreDirectorFactory<Solution_, ?> scoreDirectorFactory;

    public DefaultSolverFactory(SolverConfig solverConfig) {
        this.clock = Objects.requireNonNullElse(solverConfig.getClock(), Clock.systemDefaultZone());
        this.solverConfig = Objects.requireNonNull(solverConfig, "The solverConfig (" + solverConfig + ") cannot be null.");
        this.solutionDescriptor = buildSolutionDescriptor();
        // Caching score director factory as it potentially does expensive things.
        this.scoreDirectorFactory = buildScoreDirectorFactory();
    }

    public Clock getClock() {
        return clock;
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> ScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory() {
        return (ScoreDirectorFactory<Solution_, Score_>) scoreDirectorFactory;
    }

    @Override
    public @NonNull Solver<Solution_> buildSolver(@NonNull SolverConfigOverride<Solution_> configOverride) {
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

        var environmentMode = solverConfig.determineEnvironmentMode();
        var isStepAssertOrMore = environmentMode.isStepAssertOrMore();
        var constraintMatchEnabled = !metricsRequiringConstraintMatchSet.isEmpty() || isStepAssertOrMore;
        if (constraintMatchEnabled && !isStepAssertOrMore) {
            LOGGER.info(
                    "Enabling constraint matching as required by the enabled metrics ({}). This will impact solver performance.",
                    metricsRequiringConstraintMatchSet);
        }

        var castScoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withLookUpEnabled(true)
                .withConstraintMatchPolicy(
                        constraintMatchEnabled ? ConstraintMatchPolicy.ENABLED : ConstraintMatchPolicy.DISABLED)
                .build();
        solverScope.setScoreDirector(castScoreDirector);
        solverScope.setProblemChangeDirector(new DefaultProblemChangeDirector<>(castScoreDirector));

        var moveThreadCount = resolveMoveThreadCount(true);
        var bestSolutionRecaller = BestSolutionRecallerFactory.create().<Solution_> buildBestSolutionRecaller(environmentMode);
        var randomFactory = buildRandomFactory(environmentMode);
        var previewFeaturesEnabled = solverConfig.getEnablePreviewFeatureSet();

        var configPolicy = new HeuristicConfigPolicy.Builder<Solution_>()
                .withPreviewFeatureSet(previewFeaturesEnabled)
                .withEnvironmentMode(environmentMode)
                .withMoveThreadCount(moveThreadCount)
                .withMoveThreadBufferSize(solverConfig.getMoveThreadBufferSize())
                .withThreadFactoryClass(solverConfig.getThreadFactoryClass())
                .withNearbyDistanceMeterClass(solverConfig.getNearbyDistanceMeterClass())
                .withRandom(randomFactory.createRandom())
                .withInitializingScoreTrend(scoreDirectorFactory.getInitializingScoreTrend())
                .withSolutionDescriptor(solutionDescriptor)
                .withClassInstanceCache(ClassInstanceCache.create())
                .build();
        var basicPlumbingTermination = new BasicPlumbingTermination<Solution_>(isDaemon);
        var termination = buildTermination(basicPlumbingTermination, configPolicy, configOverride);
        var phaseList = buildPhaseList(configPolicy, bestSolutionRecaller, termination);

        return new DefaultSolver<>(environmentMode, randomFactory, bestSolutionRecaller, basicPlumbingTermination,
                (UniversalTermination<Solution_>) termination, phaseList, solverScope,
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
            HeuristicConfigPolicy<Solution_> configPolicy, SolverConfigOverride<Solution_> solverConfigOverride) {
        var terminationConfig = Objects.requireNonNullElseGet(solverConfigOverride.getTerminationConfig(),
                () -> Objects.requireNonNullElseGet(solverConfig.getTerminationConfig(), TerminationConfig::new));
        return TerminationFactory.<Solution_> create(terminationConfig)
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
                solverConfig.determineDomainAccessType(),
                (Class<Solution_>) solverConfig.getSolutionClass(),
                solverConfig.getGizmoMemberAccessorMap(),
                solverConfig.getGizmoSolutionClonerMap(),
                solverConfig.getEntityClassList());
    }

    private <Score_ extends Score<Score_>> ScoreDirectorFactory<Solution_, Score_> buildScoreDirectorFactory() {
        var environmentMode = solverConfig.determineEnvironmentMode();
        var scoreDirectorFactoryConfig_ =
                Objects.requireNonNullElseGet(solverConfig.getScoreDirectorFactoryConfig(), ScoreDirectorFactoryConfig::new);
        var scoreDirectorFactoryFactory = new ScoreDirectorFactoryFactory<Solution_, Score_>(scoreDirectorFactoryConfig_);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(environmentMode, solutionDescriptor);
    }

    public RandomFactory buildRandomFactory(EnvironmentMode environmentMode_) {
        var randomFactoryClass = solverConfig.getRandomFactoryClass();
        if (randomFactoryClass != null) {
            var randomType = solverConfig.getRandomType();
            var randomSeed = solverConfig.getRandomSeed();
            if (randomType != null || randomSeed != null) {
                throw new IllegalArgumentException(
                        "The solverConfig with randomFactoryClass (%s) has a non-null randomType (%s) or a non-null randomSeed (%s)."
                                .formatted(randomFactoryClass, randomType, randomSeed));
            }
            return ConfigUtils.newInstance(solverConfig, "randomFactoryClass", randomFactoryClass);
        } else {
            var randomType_ = Objects.requireNonNullElse(solverConfig.getRandomType(), RandomType.JDK);
            var randomSeed_ = solverConfig.getRandomSeed();
            if (solverConfig.getRandomSeed() == null && environmentMode_ != EnvironmentMode.NON_REPRODUCIBLE) {
                randomSeed_ = DEFAULT_RANDOM_SEED;
            }
            return new DefaultRandomFactory(randomType_, randomSeed_);
        }
    }

    public List<Phase<Solution_>> buildPhaseList(HeuristicConfigPolicy<Solution_> configPolicy,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, SolverTermination<Solution_> termination) {
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (ConfigUtils.isEmptyCollection(phaseConfigList)) {
            var genuineEntityDescriptorCollection = configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors();
            phaseConfigList = new ArrayList<>(genuineEntityDescriptorCollection.size() + 2);
            for (var entityDescriptor : genuineEntityDescriptorCollection) {
                if (entityDescriptor.hasBothGenuineListAndBasicVariables()) {
                    // We add a separate step for each variable type
                    phaseConfigList.add(buildConstructionHeuristicPhaseConfigForBasicVariable(configPolicy, entityDescriptor));
                    phaseConfigList.add(buildConstructionHeuristicPhaseConfigForListVariable(configPolicy, entityDescriptor));
                } else if (entityDescriptor.hasAnyGenuineListVariables()) {
                    // There is no need to revalidate the number of list variables,
                    // as it has already been validated in SolutionDescriptor
                    // TODO: Do multiple Construction Heuristics for each list variable descriptor?
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
        var listVariableDescriptor = entityDescriptor.getGenuineListVariableDescriptor();
        constructionHeuristicPhaseConfig
                .setEntityPlacerConfig(DefaultConstructionHeuristicPhaseFactory
                        .buildListVariableQueuedValuePlacerConfig(configPolicy, listVariableDescriptor));
        return constructionHeuristicPhaseConfig;
    }

    public void ensurePreviewFeature(PreviewFeature previewFeature) {
        HeuristicConfigPolicy.ensurePreviewFeature(previewFeature, solverConfig.getEnablePreviewFeatureSet());
    }

    // Required for testability as final classes cannot be mocked.
    static class MoveThreadCountResolver {

        protected OptionalInt resolveMoveThreadCount(String moveThreadCount) {
            return resolveMoveThreadCount(moveThreadCount, true);
        }

        protected OptionalInt resolveMoveThreadCount(String moveThreadCount, boolean enforceMaximum) {
            var availableProcessorCount = getAvailableProcessors();
            int resolvedMoveThreadCount;
            if (moveThreadCount == null || moveThreadCount.equals(SolverConfig.MOVE_THREAD_COUNT_NONE)) {
                return OptionalInt.empty();
            } else if (moveThreadCount.equals(SolverConfig.MOVE_THREAD_COUNT_AUTO)) {
                // Leave one for the Operating System and 1 for the solver thread, take the rest
                resolvedMoveThreadCount = (availableProcessorCount - 2);
                if (enforceMaximum && resolvedMoveThreadCount > 4) {
                    // A moveThreadCount beyond 4 is currently typically slower
                    // TODO remove limitation after fixing https://issues.redhat.com/browse/PLANNER-2449
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
