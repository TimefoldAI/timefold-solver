package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.basic;

import static ai.timefold.solver.core.impl.evolutionaryalgorithm.common.Utils.fixIndex;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.common.Utils.generateIndexes;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.applyPhases;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.updateScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.DefaultConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.custom.DefaultPhaseCommandContext;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Generates individuals for the population by applying a ruin-and-recreate method to the current best individual,
 * followed by local search.
 * <p>
 * When the population is empty the first individual is built using a deterministic best-fit construction phase,
 * identical to {@link DefaultConstructionIndividualStrategy}.
 * For every subsequent individual the strategy selects a random contiguous segment from the best individual's
 * chromosome, sets those variables to {@code null} (ruin phase), and reinitialize them via a shuffled first-fit
 * construction phase (recreate phase) before running local search. The segment boundaries are snapped to entity
 * borders so that all basic variables belonging to the same entity are always ruined or kept together.
 */
@NullMarked
public record BasicRuinRecreateIndividualStrategy<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>(
        List<PhaseCommand<Solution_>> customPhaseIndividualCommandList, Phase<Solution_> deterministicBestFitConstructionPhase,
        Phase<Solution_> shuffledFirstFitConstructionPhase, Phase<Solution_> localSearchPhase,
        @Nullable Phase<Solution_> refinementPhase, SolutionStateManager<Solution_, Score_, State_> solutionStateManager,
        IndividualBuilder<Solution_, Score_> individualBuilder,
        double inheritanceRate) implements ConstructionIndividualStrategy<Solution_, Score_> {

    public BasicRuinRecreateIndividualStrategy(List<PhaseCommand<Solution_>> customPhaseIndividualCommandList,
            Phase<Solution_> deterministicBestFitConstructionPhase,
            Phase<Solution_> shuffledFirstFitConstructionPhase,
            Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> refinementPhase,
            SolutionStateManager<Solution_, Score_, State_> solutionStateManager,
            IndividualBuilder<Solution_, Score_> individualBuilder, double inheritanceRate) {
        this.customPhaseIndividualCommandList = Objects.requireNonNull(customPhaseIndividualCommandList);
        this.deterministicBestFitConstructionPhase = Objects.requireNonNull(deterministicBestFitConstructionPhase);
        this.shuffledFirstFitConstructionPhase = Objects.requireNonNull(shuffledFirstFitConstructionPhase);
        this.localSearchPhase = Objects.requireNonNull(localSearchPhase);
        this.refinementPhase = refinementPhase;
        this.solutionStateManager = solutionStateManager;
        this.individualBuilder = Objects.requireNonNull(individualBuilder);
        this.inheritanceRate = inheritanceRate;
    }

    @Override
    public Individual<Solution_, Score_> apply(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var solverScope = phaseScope.getSolverScope();
        var scoreDirector = solverScope.<Score_> getScoreDirector();
        if (!customPhaseIndividualCommandList.isEmpty()) {
            var commandContext = new DefaultPhaseCommandContext<>(stepScope.getMoveDirector(),
                    () -> phaseScope.getTermination().isPhaseTerminated(phaseScope));
            customPhaseIndividualCommandList.forEach(command -> command.changeWorkingSolution(commandContext));
        }
        updateScope(phaseScope);
        if (stepScope.getBestIndividual() == null) {
            applyPhases(phaseScope, deterministicBestFitConstructionPhase, localSearchPhase, refinementPhase);
        } else {
            applyRuinRecreate(solverScope, scoreDirector, phaseScope, Objects.requireNonNull(stepScope.getBestIndividual()));
            updateScope(phaseScope);
            applyPhases(phaseScope, localSearchPhase, refinementPhase);
        }
        return individualBuilder.build(scoreDirector.cloneSolution(solverScope.getBestSolution()), solverScope.getBestScore(),
                null, null, scoreDirector);
    }

    @Override
    public Phase<Solution_> getLocalSearchPhase() {
        return localSearchPhase;
    }

    @Override
    public @Nullable Phase<Solution_> getRefinementPhase() {
        return refinementPhase;
    }

    void applyRuinRecreate(SolverScope<Solution_> solverScope, InnerScoreDirector<Solution_, Score_> scoreDirector,
            EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope, Individual<Solution_, Score_> bestIndividual) {
        var bestSolutionState = solutionStateManager.saveSolutionState(scoreDirector, bestIndividual);
        solutionStateManager.restoreSolutionState(scoreDirector, bestSolutionState);
        applyRuinPhase(scoreDirector, solverScope.getWorkingRandom(), bestIndividual);
        updateScope(phaseScope);
        applyPhases(phaseScope, shuffledFirstFitConstructionPhase);
    }

    private void applyRuinPhase(InnerScoreDirector<Solution_, Score_> scoreDirector,
            RandomGenerator workingRandom, Individual<Solution_, Score_> bestIndividual) {
        var chromosome = bestIndividual.getChromosome();
        var indexes = generateIndexes(workingRandom, chromosome.length, inheritanceRate, false);
        var start = fixIndex(chromosome, indexes[0], true);
        var end = fixIndex(chromosome, indexes[1], false);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var moveList = new ArrayList<Move<Solution_>>(end - start);
        EntityDescriptor<Solution_> entityDescriptor = null;
        for (var i = start; i < end; i++) {
            var entry = chromosome[i];
            if (entityDescriptor == null || entityDescriptor.getEntityClass() != entry.entity().getClass()) {
                entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entry.entity().getClass());
            }
            var rebasedEntity = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(entry.entity()));
            if (scoreDirector.getMoveDirector().isPinned(entityDescriptor, rebasedEntity)) {
                continue;
            }
            var variableDescriptor = entityDescriptor.getBasicVariableDescriptorList().get(entry.index());
            moveList.add(Moves.change(variableDescriptor.getVariableMetaModel(), rebasedEntity, null));
        }
        if (!moveList.isEmpty()) {
            scoreDirector.getMoveDirector().execute(Moves.compose(moveList));
        }
    }
}
