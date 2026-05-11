package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.bestsolution.BestSolutionUpdater;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of the foundational components of the HGS algorithm. Each worker has its own score director, allowing it to
 * apply its logic for generating and refining new individuals without affecting the state of the root solver's scope during
 * execution.
 * <p>
 * To ensure that the generation of individuals starts from a fresh phase scope, a copy of the scope is always created when
 * executing the related logic, allowing the inner phases to operate consistently. The approach described is necessary to ensure
 * consistency during the execution of the inner phases. For instance, if the {@code constructionIndividualStrategy} generates a
 * solution that is worse than the current best solution, and the {@code localSearchPhase} is subsequently applied, the LA
 * acceptance table will be initialized with the current best solution. This could lead to unexpected behavior since the method
 * should start from the solution produced by the construction strategy as the best solution.
 * 
 * @param <Solution_> the solution stype
 * @param <Score_> the score type
 * @param <State_> the solution state type
 */
@NullMarked
public class HybridGeneticSearchWorker<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>> {

    private final ConstructionIndividualStrategy<Solution_, Score_> constructionIndividualStrategy;
    private final Phase<Solution_> localSearchPhase;
    private final @Nullable Phase<Solution_> refinementPhase;
    private final CrossoverStrategy<Solution_, Score_> crossoverStrategy;
    private final IndividualBuilder<Solution_, Score_> individualBuilder;
    private final SolutionStateManager<Solution_, Score_, State_> solutionManager;
    private final BestSolutionUpdater<Solution_> bestSolutionUpdater;
    private final InnerScoreDirector<Solution_, Score_> ownScoreDirector;

    @Nullable
    private State_ initialState;

    public HybridGeneticSearchWorker(ConstructionIndividualStrategy<Solution_, Score_> constructionIndividualStrategy,
            Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> refinementPhase,
            CrossoverStrategy<Solution_, Score_> crossoverStrategy, IndividualBuilder<Solution_, Score_> individualBuilder,
            SolutionStateManager<Solution_, Score_, State_> solutionManager,
            BestSolutionUpdater<Solution_> bestSolutionUpdater, InnerScoreDirector<Solution_, Score_> ownScoreDirector) {
        this.constructionIndividualStrategy = constructionIndividualStrategy;
        this.localSearchPhase = localSearchPhase;
        this.refinementPhase = refinementPhase;
        this.crossoverStrategy = crossoverStrategy;
        this.individualBuilder = individualBuilder;
        this.solutionManager = solutionManager;
        this.bestSolutionUpdater = bestSolutionUpdater;
        this.ownScoreDirector = ownScoreDirector;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * Generate a new individual and try to update the best solution.
     *
     * @param sharedPhaseScope the shared phase scope
     * @param individualConsumer the individual consumer
     */
    public void generateIndividual(EvolutionaryAlgorithmPhaseScope<Solution_> sharedPhaseScope,
            Consumer<Individual<Solution_, Score_>> individualConsumer) {
        if (sharedPhaseScope.getTermination().isPhaseTerminated(sharedPhaseScope)) {
            return;
        }
        // The solver's working solution is restored to its initial state, and a separate phase scope is created.
        var restoredPhaseScope = restoreState(sharedPhaseScope, Objects.requireNonNull(initialState));
        var stepScope = new EvolutionaryAlgorithmStepScope<>(restoredPhaseScope);
        var newIndividual = constructionIndividualStrategy.apply(stepScope);
        var addIndividual = true;
        var oldScore = newIndividual.getScore();
        if (!newIndividual.getScore().raw().isFeasible()
                && restoredPhaseScope.getSolverScope().getWorkingRandom().nextBoolean()) {
            individualConsumer.accept(newIndividual.clone(ownScoreDirector));
            applyPhases(restoredPhaseScope, localSearchPhase, refinementPhase);
            if (newIndividual.getScore().compareTo(oldScore) == 0) {
                addIndividual = false;
            }
        }
        if (addIndividual) {
            individualConsumer.accept(newIndividual);
        }
        bestSolutionUpdater.updateBestSolution(new EvolutionaryAlgorithmStepScope<>(restoredPhaseScope, newIndividual));
    }

    /**
     * Apply the crossover operation and generates a new offspring.
     *
     * @param sharedStepScope the shared step scope
     * @param firstIndividual the first parent
     * @param secondIndividual the second parent
     * @param individualConsumer the individual consumer
     */
    public void applyCrossover(EvolutionaryAlgorithmStepScope<Solution_> sharedStepScope,
            Individual<Solution_, Score_> firstIndividual, Individual<Solution_, Score_> secondIndividual,
            Consumer<Individual<Solution_, Score_>> individualConsumer) {
        var sharedPhaseScope = sharedStepScope.getPhaseScope();
        if (sharedPhaseScope.getTermination().isPhaseTerminated(sharedPhaseScope)) {
            return;
        }
        var restoredPhaseScope = restoreState(sharedPhaseScope, Objects.requireNonNull(initialState));
        var crossoverContext = new CrossoverContext<>(restoredPhaseScope, firstIndividual, secondIndividual);
        var offspringResult = crossoverStrategy.apply(crossoverContext);
        var offspringIndividual = individualBuilder.build(offspringResult.solution(), offspringResult.score(),
                offspringResult.firstParentScore(), offspringResult.secondParentScore(), ownScoreDirector);
        var addIndividual = true;
        var oldScore = offspringIndividual.getScore();
        if (!offspringIndividual.getScore().raw().isFeasible()
                && restoredPhaseScope.getSolverScope().getWorkingRandom().nextBoolean()) {
            individualConsumer.accept(offspringIndividual.clone(ownScoreDirector));
            applyPhases(restoredPhaseScope, localSearchPhase, refinementPhase);
            if (offspringIndividual.getScore().compareTo(oldScore) == 0) {
                addIndividual = false;
            }
        }
        if (addIndividual) {
            individualConsumer.accept(offspringIndividual);
        }
        sharedStepScope.setStepIndividual(offspringIndividual);
        sharedStepScope.setScore(offspringResult.score());
    }

    /**
     * This process restores the score director's working solution to the specified state and create a separate phase scope for
     * the inner phases to work with.
     *
     * @param state the state to be restored
     */
    private EvolutionaryAlgorithmPhaseScope<Solution_> restoreState(EvolutionaryAlgorithmPhaseScope<Solution_> sharedPhaseScope,
            State_ state) {
        solutionManager.restoreSolutionState(ownScoreDirector, state);
        var restoredPhaseScope = sharedPhaseScope.copy(ownScoreDirector);
        restoredPhaseScope.getSolverScope().setBestScore(state.getScore());
        restoredPhaseScope.getSolverScope().setBestSolutionTimeMillis(restoredPhaseScope.getSolverScope().getClock().millis());
        return restoredPhaseScope;
    }

    /**
     * The restart process rebuilds and replaces the individuals of the current population.
     *
     * @param sharedPhaseScope the shared phase scope
     * @param size the size of the population
     */
    public void restartPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> sharedPhaseScope, int size,
            Consumer<Individual<Solution_, Score_>> individualConsumer) {
        var individualList = new ArrayList<Individual<Solution_, Score_>>(size);
        while (individualList.size() < size) {
            if (sharedPhaseScope.getTermination().isPhaseTerminated(sharedPhaseScope)) {
                break;
            }
            generateIndividual(sharedPhaseScope, individual -> {
                individualList.add(individual);
                individualConsumer.accept(individual);
            });
        }
        sharedPhaseScope.<Score_> getPopulation().restart(individualList);
    }

    public static <Solution_> void updateScope(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        // We need to update the best solution, best score,
        // and initialized score to avoid inconsistencies when running inner phases
        var solverScope = phaseScope.getSolverScope();
        var newBestScore = solverScope.getScoreDirector().calculateScore();
        solverScope.setBestSolution(solverScope.getScoreDirector().getWorkingSolution());
        solverScope.setBestScore(newBestScore);
        solverScope.setBestSolutionTimeMillis(solverScope.getClock().millis());
        solverScope.setStartingInitializedScore(newBestScore.raw());
        phaseScope.reset();
    }

    @SafeVarargs
    public static <Solution_> void applyPhases(AbstractPhaseScope<Solution_> phaseScope,
            @Nullable Phase<Solution_> @Nullable... phases) {
        if (phases == null) {
            return;
        }
        var solverScope = phaseScope.getSolverScope();
        switch (phases.length) {
            case 1: {
                if (phases[0] == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                    break;
                }
                phases[0].solvingStarted(solverScope);
                phases[0].solve(solverScope);
                phases[0].solvingEnded(solverScope);
                break;
            }
            case 2: {
                if (phases[0] == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                    break;
                }
                phases[0].solvingStarted(solverScope);
                phases[0].solve(solverScope);
                phases[0].solvingEnded(solverScope);
                if (phases[1] == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                    break;
                }
                phases[1].solvingStarted(solverScope);
                phases[1].solve(solverScope);
                phases[1].solvingEnded(solverScope);
                break;
            }
            case 3: {
                if (phases[0] == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                    break;
                }
                phases[0].solvingStarted(solverScope);
                phases[0].solve(solverScope);
                phases[0].solvingEnded(solverScope);
                if (phases[1] == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                    break;
                }
                phases[1].solvingStarted(solverScope);
                phases[1].solve(solverScope);
                phases[1].solvingEnded(solverScope);
                if (phases[2] == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                    break;
                }
                phases[2].solvingStarted(solverScope);
                phases[2].solve(solverScope);
                phases[2].solvingEnded(solverScope);
                break;
            }
            default: {
                // Execute all phases
                for (var phase : phases) {
                    if (phase == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                        break;
                    }
                    phase.solvingStarted(solverScope);
                    phase.solve(solverScope);
                    phase.solvingEnded(solverScope);
                }
            }
        }
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void phaseStarted(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        // The cancellation of demand is disabled, so when a resource counter reaches zero, it is not removed.
        // This allows the algorithm
        // to function without recalculating resources such as nearby matrices and value range caches.
        ownScoreDirector.getSupplyManager().disableDemandCancellation();
        // A solution that has only pinned values assigned is preferred for generating new individuals
        this.initialState = solutionManager.saveSolutionState(ownScoreDirector, false);
    }

    public void phaseEnded(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        // Enable the cancellation of demand again and cancel all to clean up the supply manager,
        // so it doesn't hold on to any resources.
        ownScoreDirector.getSupplyManager().enableDemandCancellation();
        ownScoreDirector.getSupplyManager().cancelAll();
        this.initialState = null;
    }
}
