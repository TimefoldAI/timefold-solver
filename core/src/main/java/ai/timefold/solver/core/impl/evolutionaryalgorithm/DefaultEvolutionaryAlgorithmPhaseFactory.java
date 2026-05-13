package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import static ai.timefold.solver.core.impl.AbstractFromConfigFactory.deduceEntityDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicForagerConfig;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicPickEarlyType;
import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryAlgorithmPhaseConfig;
import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryIndividualGeneratorConfig;
import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryLocalSearchConfig;
import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryPopulationConfig;
import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryWorkerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.termination.DiminishedReturnsTerminationConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedEntityPlacerFactory;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.phase.NoBestEventPhase;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.basic.BasicSolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.list.ListSolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.basic.BasicOXCrossover;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.list.ListOXCrossover;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.EvolutionaryDecider;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchDecider;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchDecider.Builder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.BasicVariableIndividual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ListVariableIndividual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.basic.BasicRuinRecreateIndividualStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.list.ListRuinRecreateIndividualStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.swapstar.ListSwapStarPhase;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.PhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class DefaultEvolutionaryAlgorithmPhaseFactory<Solution_>
        extends AbstractPhaseFactory<Solution_, EvolutionaryAlgorithmPhaseConfig> {

    public DefaultEvolutionaryAlgorithmPhaseFactory(EvolutionaryAlgorithmPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public EvolutionaryAlgorithmPhase<Solution_> buildPhase(int phaseIndex, boolean lastInitializingPhase,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            SolverTermination<Solution_> solverTermination) {
        if (solverConfigPolicy.getSolutionDescriptor().hasBothBasicAndListVariables()) {
            throw new UnsupportedOperationException("The evolutionary algorithm cannot be applied to mixed models.");
        }
        var populationConfig = phaseConfig.getPopulationConfig();
        if (populationConfig == null) {
            populationConfig = new EvolutionaryPopulationConfig();
        }
        var populationSize = Objects.requireNonNullElse(populationConfig.getPopulationSize(), 40);
        var generationSize = Objects.requireNonNullElse(populationConfig.getGenerationSize(), 20);
        var eliteGroupSize = Objects.requireNonNullElse(populationConfig.getEliteSolutionSize(), 10);
        var populationRestartCount = Objects.requireNonNullElse(populationConfig.getPopulationRestartCount(), 400);
        var workerConfig = phaseConfig.getWorkerConfig();
        if (workerConfig == null) {
            workerConfig = new EvolutionaryWorkerConfig();
        }
        var isListVariable = solverConfigPolicy.getSolutionDescriptor().hasListVariable();
        var phaseTermination = buildPhaseTermination(solverConfigPolicy, solverTermination);
        // Research has shown
        // that simpler models perform better in operations with a higher perturbation rate.
        // Conversely,
        // complex models that work with complex datasets tend
        // to be more effective with smaller perturbation rates, such as an inheritance rate of at least 95%.
        // This means that an individual will incorporate 95% of a parent's solution for crossover operations
        // or ruin only 5% of it when creating a new individual.
        boolean isComplex = phaseConfig.getComplexProblem() != null && phaseConfig.getComplexProblem();
        var evolutionaryDecider =
                buildEvolutionaryAlgorithmDecider(workerConfig, solverConfigPolicy, solverTermination, phaseTermination,
                        bestSolutionRecaller, isComplex, isListVariable, populationSize, generationSize, eliteGroupSize,
                        populationRestartCount);
        return new DefaultEvolutionaryAlgorithmPhase.Builder<>(phaseIndex, "", phaseTermination, evolutionaryDecider,
                isComplex).build();
    }

    /**
     * The method creates the evolutionary decider. By default, the Hybrid Genetic Search approach is created.
     */
    private static <Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>
            EvolutionaryDecider<Solution_, Score_>
            buildEvolutionaryAlgorithmDecider(EvolutionaryWorkerConfig workerConfig,
                    HeuristicConfigPolicy<Solution_> solverConfigPolicy, SolverTermination<Solution_> solverTermination,
                    PhaseTermination<Solution_> phaseTermination, BestSolutionRecaller<Solution_> bestSolutionRecaller,
                    boolean isComplex, boolean isListVariable, int populationSize, int generationSize, int eliteGroupSize,
                    int populationRestartCount) {

        IndividualBuilder<Solution_, Score_> individualBuilder = buildIndividualBuilder(isListVariable);
        SolutionStateManager<Solution_, Score_, State_> solutionStateManager = buildSolutionStateManager(isListVariable);
        Phase<Solution_> deterministicBestFitConstructionPhase =
                disableBestSolutionUpdate(buildDeterministicConstructionHeuristicPhase(solverConfigPolicy,
                        workerConfig.getIndividualGeneratorConfig(), solverTermination));
        Phase<Solution_> shuffledFirstFitConstructionPhase = disableBestSolutionUpdate(
                buildShuffledConstructionHeuristicPhase(solverConfigPolicy, solverTermination, isListVariable));
        Phase<Solution_> localSearchPhase =
                disableBestSolutionUpdate(buildLocalSearchPhase(solverConfigPolicy, workerConfig.getLocalSearchConfig(),
                        solverTermination, bestSolutionRecaller, isComplex, isListVariable));
        Phase<Solution_> refinmentPhase =
                disableBestSolutionUpdate(buildRefinmentPhase(solverConfigPolicy, solverTermination, isListVariable));
        ConstructionIndividualStrategy<Solution_, Score_> constructionIndividualStrategy =
                buildConstructionIndividualPhase(workerConfig, workerConfig.getIndividualGeneratorConfig(),
                        deterministicBestFitConstructionPhase, shuffledFirstFitConstructionPhase, localSearchPhase,
                        refinmentPhase, solutionStateManager, individualBuilder, isComplex, isListVariable);
        CrossoverStrategy<Solution_, Score_> crossoverStrategy =
                buildCrossoverStrategy(workerConfig.getLocalSearchConfig(), localSearchPhase, refinmentPhase, isComplex,
                        isListVariable);

        return new Builder<Solution_, Score_, State_, HybridGeneticSearchDecider<Solution_, Score_, State_>>()
                .withPopulationSize(populationSize)
                .withGenerationSize(generationSize)
                .withEliteSolutionSize(eliteGroupSize)
                .withPopulationRestartCount(populationRestartCount)
                .withConstructionIndividualStrategy(constructionIndividualStrategy)
                .withLocalSearchPhase(localSearchPhase)
                .withRefinementPhase(refinmentPhase)
                .withCrossoverStrategy(crossoverStrategy)
                .withIndividualBuilder(individualBuilder)
                .withSolutionStateManager(solutionStateManager)
                .withPhaseTermination(phaseTermination)
                .withBestSolutionRecaller(bestSolutionRecaller)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>
            SolutionStateManager<Solution_, Score_, State_> buildSolutionStateManager(boolean isListVariable) {
        if (isListVariable) {
            return (SolutionStateManager<Solution_, Score_, State_>) new ListSolutionStateManager<>();
        } else {
            return (SolutionStateManager<Solution_, Score_, State_>) new BasicSolutionStateManager<>();
        }
    }

    private static <Solution_, Score_ extends Score<Score_>> IndividualBuilder<Solution_, Score_>
            buildIndividualBuilder(boolean isListVariable) {
        if (isListVariable) {
            return (solution, score, firstParentScore, secondParentScore, scoreDirector) -> new ListVariableIndividual<>(
                    scoreDirector, solution, score, firstParentScore, secondParentScore);
        } else {
            return (solution, score, firstParentScore, secondParentScore, scoreDirector) -> new BasicVariableIndividual<>(
                    scoreDirector, solution, score, firstParentScore, secondParentScore);
        }
    }

    private static <Solution_, Score_ extends Score<Score_>> CrossoverStrategy<Solution_, Score_> buildCrossoverStrategy(
            @Nullable EvolutionaryLocalSearchConfig localSearchConfig, Phase<Solution_> localSearchPhase,
            @Nullable Phase<Solution_> refinementPhase, boolean isComplex,
            boolean isListVariable) {
        var inheritanceRate = isComplex ? 0.95 : 0.5;
        if (localSearchConfig != null && localSearchConfig.getInheritanceRate() != null) {
            inheritanceRate = localSearchConfig.getInheritanceRate();
        }
        if (isListVariable) {
            return new ListOXCrossover<>(localSearchPhase, refinementPhase, inheritanceRate, !isComplex);
        } else {
            return new BasicOXCrossover<>(localSearchPhase, refinementPhase, inheritanceRate);
        }
    }

    private static <Solution_> Phase<Solution_> buildDeterministicConstructionHeuristicPhase(
            HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            @Nullable EvolutionaryIndividualGeneratorConfig individualGeneratorConfig,
            SolverTermination<Solution_> solverTermination) {
        var constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        if (individualGeneratorConfig != null && individualGeneratorConfig.getConstructionHeuristic() != null) {
            constructionHeuristicPhaseConfig = individualGeneratorConfig.getConstructionHeuristic();
        }
        var constructionConfigPolicy = solverConfigPolicy.cloneBuilder()
                .withEnvironmentMode(EnvironmentMode.NO_ASSERT)
                .build();
        return PhaseFactory.<Solution_> create(constructionHeuristicPhaseConfig).buildPhase(0, false,
                constructionConfigPolicy, null, solverTermination);
    }

    private static <Solution_> Phase<Solution_> buildShuffledConstructionHeuristicPhase(
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, SolverTermination<Solution_> solverTermination,
            boolean isListVariable) {
        var constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        var entityPlacerConfig = DefaultConstructionHeuristicPhaseFactory.buildDefaultEntityPlacerConfig(solverConfigPolicy,
                constructionHeuristicPhaseConfig, isListVariable ? ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE
                        : ConstructionHeuristicType.ALLOCATE_ENTITY_FROM_QUEUE);
        shuffleEntityPlacerConfig(solverConfigPolicy, entityPlacerConfig);
        constructionHeuristicPhaseConfig.setEntityPlacerConfig(entityPlacerConfig);
        constructionHeuristicPhaseConfig.setForagerConfig(new ConstructionHeuristicForagerConfig()
                .withPickEarlyType(ConstructionHeuristicPickEarlyType.FIRST_FEASIBLE_SCORE_OR_NON_DETERIORATING_HARD));
        var constructionConfigPolicy = solverConfigPolicy.cloneBuilder()
                .withEnvironmentMode(EnvironmentMode.NO_ASSERT)
                .build();
        return PhaseFactory.<Solution_> create(constructionHeuristicPhaseConfig).buildPhase(0, false,
                constructionConfigPolicy, null, solverTermination);
    }

    private static <Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>
            ConstructionIndividualStrategy<Solution_, Score_>
            buildConstructionIndividualPhase(EvolutionaryWorkerConfig workerConfig,
                    @Nullable EvolutionaryIndividualGeneratorConfig individualGeneratorConfig,
                    Phase<Solution_> deterministicBestFitConstructionPhase, Phase<Solution_> shuffledFirstFitConstructionPhase,
                    Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> refinementPhase,
                    SolutionStateManager<Solution_, Score_, State_> solutionStateManager,
                    IndividualBuilder<Solution_, Score_> individualBuilder, boolean isComplex, boolean isListVariable) {
        var inheritanceRate = isComplex ? 0.95 : 0.5;
        if (individualGeneratorConfig != null && individualGeneratorConfig.getInheritanceRate() != null) {
            inheritanceRate = individualGeneratorConfig.getInheritanceRate();
        }
        List<PhaseCommand<Solution_>> customIndividualPhaseCommandList =
                buildPhaseCommandList(workerConfig, individualGeneratorConfig);
        if (isListVariable) {
            return new ListRuinRecreateIndividualStrategy<>(customIndividualPhaseCommandList,
                    deterministicBestFitConstructionPhase, localSearchPhase, refinementPhase, solutionStateManager,
                    individualBuilder, inheritanceRate);
        } else {
            return new BasicRuinRecreateIndividualStrategy<>(customIndividualPhaseCommandList,
                    deterministicBestFitConstructionPhase, shuffledFirstFitConstructionPhase, localSearchPhase, refinementPhase,
                    solutionStateManager, individualBuilder, inheritanceRate);
        }
    }

    private static <Solution_> List<PhaseCommand<Solution_>> buildPhaseCommandList(EvolutionaryWorkerConfig workerConfig,
            @Nullable EvolutionaryIndividualGeneratorConfig individualGeneratorConfig) {
        var customIndividualPhaseCommandList = Collections.<PhaseCommand<Solution_>> emptyList();
        if (individualGeneratorConfig != null && individualGeneratorConfig.getCustomPhaseCommandClassList() != null) {
            customIndividualPhaseCommandList =
                    new ArrayList<>(individualGeneratorConfig.getCustomPhaseCommandClassList().size());
            for (var customPhaseCommandClass : individualGeneratorConfig.getCustomPhaseCommandClassList()) {
                if (customPhaseCommandClass == null) {
                    throw new IllegalArgumentException("""
                            The customPhaseCommandClass (%s) cannot be null in the evolutionary custom phase config (%s).
                            Maybe there was a typo in the class name provided in the solver config XML?"""
                            .formatted(customPhaseCommandClass, workerConfig));
                }
                PhaseCommand<Solution_> customPhaseCommand =
                        ConfigUtils.newInstance(workerConfig, "customPhaseCommandClass", customPhaseCommandClass);
                ConfigUtils.applyCustomProperties(customPhaseCommand, "customPhaseCommandClass",
                        individualGeneratorConfig.getCustomProperties(), "customProperties");
                customIndividualPhaseCommandList.add(customPhaseCommand);
            }
        }
        return customIndividualPhaseCommandList;
    }

    /**
     * The method ensures that the source entity or source value selectors from the entity placer selector is shuffled,
     * allowing for the generation of different solutions whenever the phase is restarted.
     * <p>
     * The proposed approach avoids shuffling the move selector,
     * eliminating the need to generate the entire move list upfront and then randomize it.
     */
    private static <Solution_> void shuffleEntityPlacerConfig(HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            EntityPlacerConfig<?> entityPlacerConfig) {
        if (entityPlacerConfig instanceof QueuedEntityPlacerConfig queuedEntityPlacerConfig) {
            // Basic variable, then we randomize the entity selector
            var entitySelectorConfig = Objects.requireNonNullElseGet(queuedEntityPlacerConfig.getEntitySelectorConfig(),
                    () -> QueuedEntityPlacerFactory.buildEntitySelectorConfig(solverConfigPolicy, queuedEntityPlacerConfig));
            var entityDescriptor =
                    deduceEntityDescriptor(solverConfigPolicy, entitySelectorConfig,
                            Objects.requireNonNull(entitySelectorConfig).getEntityClass());
            queuedEntityPlacerConfig.setEntitySelectorConfig(entitySelectorConfig);
            shuffleEntitySelectorConfig(entitySelectorConfig);
            var moveSelectorConfigList = Objects.requireNonNullElseGet(queuedEntityPlacerConfig.getMoveSelectorConfigList(),
                    () -> QueuedEntityPlacerFactory.buildMoveSelectorConfig(solverConfigPolicy, queuedEntityPlacerConfig,
                            entityDescriptor, entitySelectorConfig));
            if (moveSelectorConfigList.size() != 1) {
                throw new IllegalStateException(
                        "Impossible state: the move configuration list %s cannot be empty or contain multiple items."
                                .formatted(moveSelectorConfigList));
            }
            queuedEntityPlacerConfig.setMoveSelectorConfigList(moveSelectorConfigList);
            var moveSelectorConfig = Objects.requireNonNull(moveSelectorConfigList.get(0));
            switch (moveSelectorConfig) {
                case ChangeMoveSelectorConfig changeMoveSelectorConfig ->
                    shuffleValueSelectorConfig(Objects.requireNonNull(changeMoveSelectorConfig.getValueSelectorConfig()));
                case CartesianProductMoveSelectorConfig cartesianProductMoveSelectorConfig -> {
                    for (var innerMoveSelectorConfig : Objects
                            .requireNonNull(cartesianProductMoveSelectorConfig.getMoveSelectorList())) {
                        if (!(innerMoveSelectorConfig instanceof ChangeMoveSelectorConfig changeMoveSelectorConfig)) {
                            throw new IllegalStateException(
                                    "Impossible state: the inner move configration (%s) must match the type (%s)"
                                            .formatted(innerMoveSelectorConfig,
                                                    ChangeMoveSelectorConfig.class.getSimpleName()));
                        }
                        shuffleValueSelectorConfig(Objects.requireNonNull(changeMoveSelectorConfig.getValueSelectorConfig()));
                    }
                }
                default ->
                    throw new IllegalStateException("Impossible state: the move configration (%s) must match the types (%s, %s)"
                            .formatted(moveSelectorConfig, ChangeMoveSelectorConfig.class.getSimpleName(),
                                    CartesianProductMoveSelectorConfig.class.getSimpleName()));
            }
        } else if (entityPlacerConfig instanceof QueuedValuePlacerConfig queuedValuePlacerConfig) {
            // List variable, then we shuffle the source value selector
            var valueSelectorConfig = Objects.requireNonNull(queuedValuePlacerConfig.getValueSelectorConfig());
            shuffleValueSelectorConfig(valueSelectorConfig);
            // The move list has only one list change move
            var moveSelectorConfig = Objects.requireNonNull(queuedValuePlacerConfig.getMoveSelectorConfig());
            if (!(moveSelectorConfig instanceof ListChangeMoveSelectorConfig listChangeMoveSelectorConfig)) {
                throw new IllegalStateException("Impossible state: the move configration (%s) must match the type (%s)"
                        .formatted(moveSelectorConfig, ListChangeMoveSelectorConfig.class.getSimpleName()));
            }
            var destinationSelectorConfig = Objects.requireNonNullElseGet(
                    listChangeMoveSelectorConfig.getDestinationSelectorConfig(), DestinationSelectorConfig::new);
            listChangeMoveSelectorConfig.setDestinationSelectorConfig(destinationSelectorConfig);
            var entitySelectorConfig = Objects.requireNonNullElseGet(destinationSelectorConfig.getEntitySelectorConfig(),
                    EntitySelectorConfig::new);
            destinationSelectorConfig.setEntitySelectorConfig(entitySelectorConfig);
        }
    }

    private static void shuffleEntitySelectorConfig(EntitySelectorConfig entitySelectorConfig) {
        Objects.requireNonNull(entitySelectorConfig).setSelectionOrder(SelectionOrder.SHUFFLED);
        Objects.requireNonNull(entitySelectorConfig).setCacheType(SelectionCacheType.PHASE);
    }

    private static void shuffleValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        Objects.requireNonNull(valueSelectorConfig).setSelectionOrder(SelectionOrder.SHUFFLED);
        Objects.requireNonNull(valueSelectorConfig).setCacheType(SelectionCacheType.PHASE);
    }

    /**
     * The method creates a local search phase based on Diversified Late Acceptance and customized diminished termination.
     */
    private static <Solution_> Phase<Solution_> buildLocalSearchPhase(HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            @Nullable EvolutionaryLocalSearchConfig localSearchPhaseConfig, SolverTermination<Solution_> solverTermination,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, boolean isComplex, boolean isListVariable) {
        var updatedLocalSearchPhaseConfig = localSearchPhaseConfig != null ? localSearchPhaseConfig.getLocalSearch() : null;
        if (updatedLocalSearchPhaseConfig == null) {
            updatedLocalSearchPhaseConfig = new LocalSearchPhaseConfig();
            updatedLocalSearchPhaseConfig.setLocalSearchType(LocalSearchType.DIVERSIFIED_LATE_ACCEPTANCE);
        }
        if (updatedLocalSearchPhaseConfig.getTerminationConfig() == null) {
            var terminationConfig = new TerminationConfig();
            var windowTime = isComplex ? 20L : 1L;
            terminationConfig.setDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig()
                    .withMinimumImprovementRatio(0.01).withSlidingWindowSeconds(windowTime));
            updatedLocalSearchPhaseConfig.setTerminationConfig(terminationConfig);
        }
        var clearNearbyClass = updatedLocalSearchPhaseConfig.getMoveSelectorConfig() == null;
        loadMoveSelectorConfig(solverConfigPolicy, updatedLocalSearchPhaseConfig, isListVariable);
        var localSearchConfigPolicy = solverConfigPolicy.cloneBuilder()
                .withEnvironmentMode(EnvironmentMode.NO_ASSERT)
                .withPreviewFeature(PreviewFeature.DIVERSIFIED_LATE_ACCEPTANCE);
        if (clearNearbyClass) {
            localSearchConfigPolicy.withNearbyDistanceMeterClass(null);
        }
        return PhaseFactory.<Solution_> create(updatedLocalSearchPhaseConfig).buildPhase(0, false,
                localSearchConfigPolicy.build(), bestSolutionRecaller, solverTermination);
    }

    /**
     * The move config for list variable includes all move types used by the HGS original article:
     * 2.1 - Reallocate planning value U after a planning value V (regular change and list change moves)
     * 2.2 - Swap planning value U with a planning value V (regular swap and list swap moves)
     * 2.3 - Reallocate planning value U and its successor X after a planning value V: (U, X) -> Entity[position]
     * 2.4 - Reallocate planning value U and its successor X after a planning value V, and invert the values: (X, U) ->
     * Entity[position]
     * 2.5 - Swap two planning values U and X with a planning value: (U, X) <-> V
     * 2.6 - Swap two planning values U and X with a planning value V, and invert the values: (X, U) <-> V
     * 2.7 - Intra route 2-opt move: (U, X) (V, Y) -> (U, V) (X, Y)
     * 2.8 - Inter route 2-opt move: (U, X) (V, Y) -> (U, V) (X, Y)
     * 2.9 - Inter route 2-opt move: (U, X) (V, Y) -> (U, Y) (V, X)
     * <p>
     * As for the basic variables, four types of moves are included: change move, swap move, pillar change move, and pillar swap
     * move.
     */
    private static <Solution_> void loadMoveSelectorConfig(HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            LocalSearchPhaseConfig localSearchPhaseConfig, boolean isListVariable) {
        if (localSearchPhaseConfig.getMoveSelectorConfig() == null) {
            var updatedUnionMoveSelectorConfig = new UnionMoveSelectorConfig();
            var moveList = new ArrayList<MoveSelectorConfig>();
            if (isListVariable) {
                // Move 2.1
                moveList.add(new ListChangeMoveSelectorConfig());
                // Move 2.2
                moveList.add(new ListSwapMoveSelectorConfig());
                // Move 2.3 and 2.4
                moveList.add(new SubListChangeMoveSelectorConfig()
                        .withSelectReversingMoveToo(true).withSubListSelectorConfig(
                                new SubListSelectorConfig().withMinimumSubListSize(2).withMaximumSubListSize(2)));
                // Move 2.5 and 2.6
                moveList.add(new SubListSwapMoveSelectorConfig()
                        .withSelectReversingMoveToo(true)
                        .withSubListSelectorConfig(
                                new SubListSelectorConfig().withMinimumSubListSize(2).withMaximumSubListSize(2))
                        .withSecondarySubListSelectorConfig(
                                new SubListSelectorConfig().withMinimumSubListSize(1).withMaximumSubListSize(1)));
                // Moves 2.7, 2.8 and 2.9
                moveList.add(new KOptListMoveSelectorConfig().withMinimumK(2).withMaximumK(2));
            } else {
                moveList.add(new ChangeMoveSelectorConfig());
                moveList.add(new SwapMoveSelectorConfig());
                var solutionDescriptor = solverConfigPolicy.getSolutionDescriptor();
                // The pillar movement should include additional information when multiple variables are involved
                for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
                    for (var variableDescriptor : entityDescriptor.getBasicVariableDescriptorList()) {
                        moveList.add(new PillarChangeMoveSelectorConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig()
                                        .withVariableName(variableDescriptor.getVariableName()))
                                .withPillarSelectorConfig(new PillarSelectorConfig()
                                        .withEntitySelectorConfig(
                                                new EntitySelectorConfig().withEntityClass(entityDescriptor.getEntityClass()))
                                        .withMinimumSubPillarSize(2)
                                        .withMaximumSubPillarSize(2)));
                    }
                    moveList.add(
                            new PillarSwapMoveSelectorConfig()
                                    .withPillarSelectorConfig(new PillarSelectorConfig()
                                            .withEntitySelectorConfig(new EntitySelectorConfig()
                                                    .withEntityClass(entityDescriptor.getEntityClass()))
                                            .withMinimumSubPillarSize(2)
                                            .withMaximumSubPillarSize(2)));
                }
            }
            updatedUnionMoveSelectorConfig.setMoveSelectorList(moveList);
            localSearchPhaseConfig.setMoveSelectorConfig(updatedUnionMoveSelectorConfig);
        }
        if (solverConfigPolicy.getNearbyDistanceMeterClass() != null && localSearchPhaseConfig
                .getMoveSelectorConfig() instanceof NearbyAutoConfigurationEnabled<?> nearbyAutoConfiguration) {
            var nearbyDistanceMeterClass =
                    (Class<? extends NearbyDistanceMeter<?, ?>>) solverConfigPolicy.getNearbyDistanceMeterClass();
            // The article uses a granular neighborhood with 20 closest customers, 
            // but some experiments have shown that relying solely on a neighborhood with nearby feature enabled can be counterproductive, 
            // potentially preventing the solver from exploring better areas of the solution space. 
            // Additionally, 
            // the local search phase used by the method, 
            // by default, 
            // does not depend on the size of the neighborhood to complete the refinement step.
            var updatedUnionMoveSelectorConfig =
                    nearbyAutoConfiguration.enableNearbySelection(nearbyDistanceMeterClass, solverConfigPolicy.getRandom());
            localSearchPhaseConfig.setMoveSelectorConfig(updatedUnionMoveSelectorConfig);
        }
    }

    /**
     * The method creates an optimization phase
     * to implement the SWAP* approach for list variables as outlined in the HGS article.
     */
    private static <Solution_> @Nullable Phase<Solution_> buildRefinmentPhase(
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, SolverTermination<Solution_> solverTermination,
            boolean isListVariable) {
        if (isListVariable && solverConfigPolicy.getNearbyDistanceMeterClass() != null) {
            var entityClass = solverConfigPolicy.getSolutionDescriptor().getListVariableDescriptor().getEntityDescriptor()
                    .getEntityClass();
            var originalEntitySelectorConfig = new EntitySelectorConfig()
                    .withId(ConfigUtils.addRandomSuffix(entityClass.getName(), solverConfigPolicy.getRandom()))
                    .withEntityClass(entityClass);
            var originalEntitySelector = EntitySelectorFactory.<Solution_> create(originalEntitySelectorConfig)
                    .buildEntitySelector(solverConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL);
            var innerEntitySelectorConfig = new EntitySelectorConfig()
                    .withNearbySelectionConfig(new NearbySelectionConfig()
                            .withOriginEntitySelectorConfig(
                                    EntitySelectorConfig.newMimicSelectorConfig(
                                            Objects.requireNonNull(originalEntitySelectorConfig.getId())))
                            .withNearbyDistanceMeterClass(solverConfigPolicy.getNearbyDistanceMeterClass()));
            var innerEntitySelector = EntitySelectorFactory.<Solution_> create(innerEntitySelectorConfig)
                    .buildEntitySelector(solverConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL);

            return new ListSwapStarPhase.Builder<>(0, "", PhaseTermination.bridge(solverTermination), originalEntitySelector,
                    innerEntitySelector).build();
        }
        return null;
    }

    /**
     * Utility method that disables updates to the best solution events during the evolutionary inner phases.
     * 
     * @param phase the phase to be configured
     * @return a phase that disables the best solution updates and run the same logic as the inner phase.
     */
    private static <Solution_> @Nullable Phase<Solution_> disableBestSolutionUpdate(@Nullable Phase<Solution_> phase) {
        if (phase == null) {
            return null;
        }
        return new NoBestEventPhase<>(phase);
    }
}
