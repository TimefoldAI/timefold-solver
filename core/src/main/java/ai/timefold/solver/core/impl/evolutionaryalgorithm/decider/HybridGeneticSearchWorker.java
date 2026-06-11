package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.bestsolution.BestSolutionUpdater;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.random.DelegatingSplittableRandomGenerator;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of the foundational components of the HGS algorithm.
 * Each worker has its own solver scope, allowing it to
 * apply its logic for generating and refining new individuals
 * without affecting the state of the root solver's scope during
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

    private final HybridGeneticSearchWorkerContext<Solution_, Score_, State_> context;
    private final BestSolutionUpdater<Solution_> bestSolutionUpdater;
    private final RandomGenerator workerRandom;
    private final SolverScope<Solution_> ownSolverScope;

    @Nullable
    private State_ initialState;

    public HybridGeneticSearchWorker(HybridGeneticSearchWorkerContext<Solution_, Score_, State_> context,
            BestSolutionUpdater<Solution_> bestSolutionUpdater, DelegatingSplittableRandomGenerator workingRandom,
            SolverScope<Solution_> ownSolverScope) {
        this.context = context;
        this.bestSolutionUpdater = bestSolutionUpdater;
        this.workerRandom = workingRandom.split();
        this.ownSolverScope = ownSolverScope;
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
            Supplier<@Nullable Individual<Solution_, Score_>> bestSolutionSupplier,
            Consumer<Individual<Solution_, Score_>> individualConsumer) {
        if (sharedPhaseScope.getTermination().isPhaseTerminated(sharedPhaseScope)) {
            return;
        }
        // The solver's working solution is restored to its initial state, and a separate phase scope is created.
        var restoredPhaseScope = restoreState(sharedPhaseScope, Objects.requireNonNull(initialState));
        var stepScope = new EvolutionaryAlgorithmStepScope<>(restoredPhaseScope);
        stepScope.setBestIndividual(bestSolutionSupplier.get());
        var constructionStrategy = pickConstructionIndividualStrategy();
        var newIndividual = constructionStrategy.apply(stepScope);
        var addIndividual = true;
        var oldScore = newIndividual.getScore();
        if (!newIndividual.getScore().raw().isFeasible() && workerRandom.nextBoolean()) {
            var clonedIndividual = newIndividual.clone(ownSolverScope.getScoreDirector());
            individualConsumer.accept(clonedIndividual);
            applyPhases(restoredPhaseScope, constructionStrategy.getLocalSearchPhase(),
                    constructionStrategy.getRefinementPhase());
            if (restoredPhaseScope.<Score_> getBestScore().compareTo(oldScore) <= 0) {
                addIndividual = false;
                newIndividual = clonedIndividual;
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
        var crossoverStrategy = pickCrossoverStrategy();
        var offspringResult = crossoverStrategy.apply(crossoverContext);
        var offspringIndividual = context.individualBuilder().build(offspringResult.solution(), offspringResult.score(),
                offspringResult.firstParentScore(), offspringResult.secondParentScore(), ownSolverScope.getScoreDirector());
        var addIndividual = true;
        var oldScore = offspringIndividual.getScore();
        if (!offspringIndividual.getScore().raw().isFeasible() && workerRandom.nextBoolean()) {
            individualConsumer.accept(offspringIndividual.clone(ownSolverScope.getScoreDirector()));
            applyPhases(restoredPhaseScope, crossoverStrategy.getLocalSearchPhase(), crossoverStrategy.getRefinementPhase());
            if (restoredPhaseScope.<Score_> getBestScore().compareTo(oldScore) == 0) {
                addIndividual = false;
            }
        }
        if (addIndividual) {
            var otherOffspringIndividual = context.individualBuilder().build(
                    restoredPhaseScope.getSolverScope().getBestSolution(), restoredPhaseScope.getBestScore(),
                    offspringResult.firstParentScore(), offspringResult.secondParentScore(), ownSolverScope.getScoreDirector());
            individualConsumer.accept(otherOffspringIndividual);
            sharedStepScope.setStepIndividual(otherOffspringIndividual);
            sharedStepScope.setScore(otherOffspringIndividual.getScore());
            bestSolutionUpdater
                    .updateBestSolution(new EvolutionaryAlgorithmStepScope<>(restoredPhaseScope, otherOffspringIndividual));
        } else {
            sharedStepScope.setStepIndividual(offspringIndividual);
            sharedStepScope.setScore(offspringIndividual.getScore());
            bestSolutionUpdater
                    .updateBestSolution(new EvolutionaryAlgorithmStepScope<>(restoredPhaseScope, offspringIndividual));
        }
    }

    /**
     * This process restores the score director's working solution to the specified state and create a separate phase scope for
     * the inner phases to work with.
     *
     * @param state the state to be restored
     */
    protected EvolutionaryAlgorithmPhaseScope<Solution_> restoreState(
            EvolutionaryAlgorithmPhaseScope<Solution_> sharedPhaseScope,
            State_ state) {
        context.solutionStateManager().restoreSolutionState(ownSolverScope.getScoreDirector(), state);
        var restoredPhaseScope = sharedPhaseScope.createChildThreadPhaseScope(ownSolverScope);
        restoredPhaseScope.getSolverScope().setBestScore(state.getScore());
        restoredPhaseScope.getSolverScope().setBestSolutionTimeMillis(restoredPhaseScope.getSolverScope().getClock().millis());
        // The best solution events are disabled while the inner phases are being executed
        restoredPhaseScope.getSolverScope().setTriggerBestSolutionEvent(false);
        return restoredPhaseScope;
    }

    /**
     * The restart process rebuilds and replaces the individuals of the current population.
     *
     * @param sharedPhaseScope the shared phase scope
     * @param size the size of the population
     */
    public void restartPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> sharedPhaseScope, int size,
            Supplier<@Nullable Individual<Solution_, Score_>> bestSolutionSupplier,
            Consumer<Individual<Solution_, Score_>> individualConsumer) {
        var individualList = new ArrayList<Individual<Solution_, Score_>>(size);
        while (individualList.size() < size) {
            if (sharedPhaseScope.getTermination().isPhaseTerminated(sharedPhaseScope)) {
                break;
            }
            generateIndividual(sharedPhaseScope, bestSolutionSupplier, individual -> {
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
                applyPhases(phaseScope, phases[0]);
                break;
            }
            case 2: {
                applyPhases(phaseScope, phases[0], phases[1]);
                break;
            }
            case 3: {
                applyPhases(phaseScope, phases[0], phases[1], phases[2]);
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

    private static <Solution_> void applyPhases(AbstractPhaseScope<Solution_> phaseScope,
            @Nullable Phase<Solution_> phase) {
        var solverScope = phaseScope.getSolverScope();
        if (phase == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
            return;
        }
        phase.solvingStarted(solverScope);
        phase.solve(solverScope);
        phase.solvingEnded(solverScope);
    }

    public static <Solution_> void applyPhases(AbstractPhaseScope<Solution_> phaseScope, @Nullable Phase<Solution_> firstPhase,
            @Nullable Phase<Solution_> secondPhase) {
        var solverScope = phaseScope.getSolverScope();
        if (firstPhase == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
            return;
        }
        firstPhase.solvingStarted(solverScope);
        firstPhase.solve(solverScope);
        firstPhase.solvingEnded(solverScope);
        if (secondPhase == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
            return;
        }
        secondPhase.solvingStarted(solverScope);
        secondPhase.solve(solverScope);
        secondPhase.solvingEnded(solverScope);
    }

    public static <Solution_> void applyPhases(AbstractPhaseScope<Solution_> phaseScope, @Nullable Phase<Solution_> firstPhase,
            @Nullable Phase<Solution_> secondPhase, @Nullable Phase<Solution_> thirdPhase) {
        var solverScope = phaseScope.getSolverScope();
        if (firstPhase == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
            return;
        }
        firstPhase.solvingStarted(solverScope);
        firstPhase.solve(solverScope);
        firstPhase.solvingEnded(solverScope);
        if (secondPhase == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
            return;
        }
        secondPhase.solvingStarted(solverScope);
        secondPhase.solve(solverScope);
        secondPhase.solvingEnded(solverScope);
        if (thirdPhase == null || phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
            return;
        }
        thirdPhase.solvingStarted(solverScope);
        thirdPhase.solve(solverScope);
        thirdPhase.solvingEnded(solverScope);
    }

    private ConstructionIndividualStrategy<Solution_, Score_> pickConstructionIndividualStrategy() {
        if (workerRandom.nextDouble(1) < context.exploratoryRate()) {
            return context.exploratoryConstructionIndividualStrategy();
        } else {
            return context.conservativeConstructionIndividualStrategy();
        }
    }

    private CrossoverStrategy<Solution_, Score_> pickCrossoverStrategy() {
        if (workerRandom.nextDouble(1) < context.exploratoryRate()) {
            return context.exploratoryCrossoverStrategy();
        } else {
            return context.conservativeCrossoverStrategy();
        }
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void phaseStarted(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        // The cancellation of demand is disabled, so when a resource counter reaches zero, it is not removed.
        // This allows the algorithm
        // to function without recalculating resources such as nearby matrices and value range caches.
        this.ownSolverScope.getScoreDirector().getSupplyManager().disableDemandCancellation();
        // A solution that has only pinned values assigned is preferred for generating new individuals
        this.initialState = context.solutionStateManager().saveSolutionState(ownSolverScope.getScoreDirector(), false);
    }

    public void phaseEnded(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        // Enable the cancellation of demand again and cancel all to clean up the supply manager,
        // so it doesn't hold on to any resources.
        ownSolverScope.getScoreDirector().getSupplyManager().enableDemandCancellation();
        ownSolverScope.getScoreDirector().getSupplyManager().cancelAll();
        this.initialState = null;
    }
}
