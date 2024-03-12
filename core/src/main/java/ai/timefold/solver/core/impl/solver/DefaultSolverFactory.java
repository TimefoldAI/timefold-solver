package ai.timefold.solver.core.impl.solver;

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
import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.random.RandomType;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.AbstractFromConfigFactory;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.PhaseFactory;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.random.DefaultRandomFactory;
import ai.timefold.solver.core.impl.solver.random.RandomFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecallerFactory;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.Termination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;

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

    private final SolverConfig solverConfig;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final InnerScoreDirectorFactory<Solution_, ?> scoreDirectorFactory;

    public DefaultSolverFactory(SolverConfig solverConfig) {
        this.solverConfig = Objects.requireNonNull(solverConfig, "The solverConfig (" + solverConfig + ") cannot be null.");
        this.solutionDescriptor = buildSolutionDescriptor();
        // Caching score director factory as it potentially does expensive things.
        this.scoreDirectorFactory = buildScoreDirectorFactory();
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    public <Score_ extends Score<Score_>> InnerScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory() {
        return (InnerScoreDirectorFactory<Solution_, Score_>) scoreDirectorFactory;
    }

    @Override
    public Solver<Solution_> buildSolver(SolverConfigOverride<Solution_> configOverride) {
        Objects.requireNonNull(configOverride, "Invalid configOverride (null) given to SolverFactory.");
        var isDaemon = Objects.requireNonNullElse(solverConfig.getDaemon(), false);

        var solverScope = new SolverScope<Solution_>();
        var monitoringConfig = solverConfig.determineMetricConfig();
        solverScope.setMonitoringTags(Tags.empty());
        var metricsRequiringConstraintMatchSet = Collections.<SolverMetric> emptyList();
        if (!monitoringConfig.getSolverMetricList().isEmpty()) {
            solverScope.setSolverMetricSet(EnumSet.copyOf(monitoringConfig.getSolverMetricList()));
            metricsRequiringConstraintMatchSet = solverScope.getSolverMetricSet().stream()
                    .filter(SolverMetric::isMetricConstraintMatchBased)
                    .filter(solverScope::isMetricEnabled)
                    .toList();
        } else {
            solverScope.setSolverMetricSet(EnumSet.noneOf(SolverMetric.class));
        }

        var environmentMode = solverConfig.determineEnvironmentMode();
        var constraintMatchEnabled = !metricsRequiringConstraintMatchSet.isEmpty() || environmentMode.isAsserted();
        if (constraintMatchEnabled && !environmentMode.isAsserted()) {
            LOGGER.info(
                    "Enabling constraint matching as required by the enabled metrics ({}). This will impact solver performance.",
                    metricsRequiringConstraintMatchSet);
        }

        var innerScoreDirector = scoreDirectorFactory.buildScoreDirector(true, constraintMatchEnabled);
        solverScope.setScoreDirector(innerScoreDirector);
        solverScope.setProblemChangeDirector(new DefaultProblemChangeDirector<>(innerScoreDirector));

        var moveThreadCount = resolveMoveThreadCount(true);
        var bestSolutionRecaller = BestSolutionRecallerFactory.create().<Solution_> buildBestSolutionRecaller(environmentMode);
        var randomFactory = buildRandomFactory(environmentMode);

        var configPolicy = new HeuristicConfigPolicy.Builder<>(
                environmentMode,
                moveThreadCount,
                solverConfig.getMoveThreadBufferSize(),
                solverConfig.getThreadFactoryClass(),
                solverConfig.getNearbyDistanceMeterClass(),
                randomFactory.createRandom(),
                scoreDirectorFactory.getInitializingScoreTrend(),
                solutionDescriptor,
                ClassInstanceCache.create()).build();
        var basicPlumbingTermination = new BasicPlumbingTermination<Solution_>(isDaemon);
        var termination = buildTerminationConfig(basicPlumbingTermination, configPolicy, configOverride);
        var phaseList = buildPhaseList(configPolicy, bestSolutionRecaller, termination);

        return new DefaultSolver<>(environmentMode, randomFactory, bestSolutionRecaller, basicPlumbingTermination,
                termination, phaseList, solverScope,
                moveThreadCount == null ? SolverConfig.MOVE_THREAD_COUNT_NONE : Integer.toString(moveThreadCount));
    }

    public Integer resolveMoveThreadCount(boolean enforceMaximum) {
        var maybeCount =
                new MoveThreadCountResolver().resolveMoveThreadCount(solverConfig.getMoveThreadCount(), enforceMaximum);
        if (maybeCount.isPresent()) {
            return maybeCount.getAsInt();
        } else {
            return null;
        }
    }

    private Termination<Solution_> buildTerminationConfig(BasicPlumbingTermination<Solution_> basicPlumbingTermination,
            HeuristicConfigPolicy<Solution_> configPolicy,
            SolverConfigOverride<Solution_> solverConfigOverride) {
        var terminationConfig = Objects.requireNonNullElseGet(solverConfig.getTerminationConfig(), TerminationConfig::new);
        if (solverConfigOverride.getTerminationConfig() != null) {
            terminationConfig = solverConfigOverride.getTerminationConfig();
        }
        return TerminationFactory.<Solution_> create(terminationConfig)
                .buildTermination(configPolicy, basicPlumbingTermination);
    }

    private SolutionDescriptor<Solution_> buildSolutionDescriptor() {
        if (solverConfig.getSolutionClass() == null) {
            throw new IllegalArgumentException("The solver configuration must have a solutionClass (" +
                    solverConfig.getSolutionClass() +
                    "). If you're using the Quarkus extension or Spring Boot starter, it should have been filled in " +
                    "already.");
        }
        if (ConfigUtils.isEmptyCollection(solverConfig.getEntityClassList())) {
            throw new IllegalArgumentException("The solver configuration must have at least 1 entityClass (" +
                    solverConfig.getEntityClassList() + "). If you're using the Quarkus extension or Spring Boot starter, " +
                    "it should have been filled in already.");
        }
        SolutionDescriptor<Solution_> solutionDescriptor =
                SolutionDescriptor.buildSolutionDescriptor(solverConfig.determineDomainAccessType(),
                        (Class<Solution_>) solverConfig.getSolutionClass(),
                        solverConfig.getGizmoMemberAccessorMap(),
                        solverConfig.getGizmoSolutionClonerMap(),
                        solverConfig.getEntityClassList());
        EnvironmentMode environmentMode = solverConfig.determineEnvironmentMode();
        if (environmentMode.isAsserted()) {
            solutionDescriptor.setAssertModelForCloning(true);
        }

        return solutionDescriptor;
    }

    private InnerScoreDirectorFactory<Solution_, ?> buildScoreDirectorFactory() {
        EnvironmentMode environmentMode = solverConfig.determineEnvironmentMode();
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig_ =
                Objects.requireNonNullElseGet(solverConfig.getScoreDirectorFactoryConfig(),
                        ScoreDirectorFactoryConfig::new);
        ScoreDirectorFactoryFactory<Solution_, ?> scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<>(scoreDirectorFactoryConfig_);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(environmentMode, solutionDescriptor);
    }

    public RandomFactory buildRandomFactory(EnvironmentMode environmentMode_) {
        RandomFactory randomFactory;
        if (solverConfig.getRandomFactoryClass() != null) {
            if (solverConfig.getRandomType() != null || solverConfig.getRandomSeed() != null) {
                throw new IllegalArgumentException(
                        "The solverConfig with randomFactoryClass (" + solverConfig.getRandomFactoryClass()
                                + ") has a non-null randomType (" + solverConfig.getRandomType()
                                + ") or a non-null randomSeed (" + solverConfig.getRandomSeed() + ").");
            }
            randomFactory = ConfigUtils.newInstance(solverConfig, "randomFactoryClass", solverConfig.getRandomFactoryClass());
        } else {
            RandomType randomType_ = Objects.requireNonNullElse(solverConfig.getRandomType(), RandomType.JDK);
            Long randomSeed_ = solverConfig.getRandomSeed();
            if (solverConfig.getRandomSeed() == null && environmentMode_ != EnvironmentMode.NON_REPRODUCIBLE) {
                randomSeed_ = DEFAULT_RANDOM_SEED;
            }
            randomFactory = new DefaultRandomFactory(randomType_, randomSeed_);
        }
        return randomFactory;
    }

    public List<Phase<Solution_>> buildPhaseList(HeuristicConfigPolicy<Solution_> configPolicy,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, Termination<Solution_> termination) {
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (ConfigUtils.isEmptyCollection(phaseConfigList)) {
            var genuineEntityDescriptorCollection = configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors();
            var listVariableDescriptor = configPolicy.getSolutionDescriptor().getListVariableDescriptor();
            var entityClassToListVariableDescriptorListMap =
                    listVariableDescriptor == null ? Collections.<Class<?>, List<ListVariableDescriptor<Solution_>>> emptyMap()
                            : Collections.singletonMap(listVariableDescriptor.getEntityDescriptor().getEntityClass(),
                                    List.of(listVariableDescriptor));

            phaseConfigList = new ArrayList<>(genuineEntityDescriptorCollection.size() + 1);
            for (var genuineEntityDescriptor : genuineEntityDescriptorCollection) {
                var constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
                EntityPlacerConfig<?> entityPlacerConfig;

                if (entityClassToListVariableDescriptorListMap.containsKey(genuineEntityDescriptor.getEntityClass())) {
                    var listVariableDescriptorList =
                            entityClassToListVariableDescriptorListMap.get(genuineEntityDescriptor.getEntityClass());
                    if (listVariableDescriptorList.size() != 1) {
                        // TODO: Do multiple Construction Heuristics for each list variable descriptor?
                        throw new IllegalArgumentException(
                                "Construction Heuristic phase does not support multiple list variables ("
                                        + listVariableDescriptorList + ") for planning entity (" +
                                        genuineEntityDescriptor.getEntityClass() + ").");
                    }
                    entityPlacerConfig =
                            DefaultConstructionHeuristicPhaseFactory.buildListVariableQueuedValuePlacerConfig(configPolicy,
                                    listVariableDescriptorList.get(0));
                } else {
                    entityPlacerConfig = new QueuedEntityPlacerConfig().withEntitySelectorConfig(AbstractFromConfigFactory
                            .getDefaultEntitySelectorConfigForEntity(configPolicy, genuineEntityDescriptor));
                }

                constructionHeuristicPhaseConfig.setEntityPlacerConfig(entityPlacerConfig);
                phaseConfigList.add(constructionHeuristicPhaseConfig);
            }
            phaseConfigList.add(new LocalSearchPhaseConfig());
        }
        return PhaseFactory.buildPhases(phaseConfigList, configPolicy, bestSolutionRecaller, termination);
    }

    // Required for testability as final classes cannot be mocked.
    static class MoveThreadCountResolver {

        protected OptionalInt resolveMoveThreadCount(String moveThreadCount) {
            return resolveMoveThreadCount(moveThreadCount, true);
        }

        protected OptionalInt resolveMoveThreadCount(String moveThreadCount, boolean enforceMaximum) {
            int availableProcessorCount = getAvailableProcessors();
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
                throw new IllegalArgumentException("The moveThreadCount (" + moveThreadCount
                        + ") resulted in a resolvedMoveThreadCount (" + resolvedMoveThreadCount
                        + ") that is lower than 1.");
            }
            if (resolvedMoveThreadCount > availableProcessorCount) {
                LOGGER.warn("The resolvedMoveThreadCount ({}) is higher "
                        + "than the availableProcessorCount ({}), which is counter-efficient.",
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
