package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultConstructionIndividualStrategyTest {

    private EvolutionaryAlgorithmStepScope<TestdataListSolution> prepareStepScope() {
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(EvolutionaryAlgorithmPhaseScope.class);
        doReturn(solverScope).when(phaseScope).getSolverScope();
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var heuristicConfigPolicy =
                new HeuristicConfigPolicy.Builder<TestdataListSolution>().withSolutionDescriptor(solutionDescriptor).build();
        var termination = (PhaseTermination<TestdataListSolution>) TerminationFactory
                .<TestdataListSolution> create(new TerminationConfig().withStepCountLimit(1))
                .buildTermination(heuristicConfigPolicy);
        doReturn(termination).when(phaseScope).getTermination();
        var stepScope = mock(EvolutionaryAlgorithmStepScope.class);
        doReturn(phaseScope).when(stepScope).getPhaseScope();
        var population = mock(Population.class);
        doReturn(population).when(phaseScope).getPopulation();
        var scoreDirector = mockScoreDirector(solutionDescriptor);
        var problem = TestdataListSolution.generateInitializedSolution(1, 1);
        scoreDirector.setWorkingSolution(problem);
        var score = scoreDirector.calculateScore();
        doReturn(scoreDirector).when(solverScope).getScoreDirector();
        doReturn(problem).when(solverScope).getBestSolution();
        doReturn(score).when(solverScope).getBestScore();
        doReturn(Clock.systemDefaultZone()).when(solverScope).getClock();
        var solver = mock(AbstractSolver.class);
        doReturn(solver).when(solverScope).getSolver();
        return stepScope;
    }

    @Test
    void apply() {
        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var deterministicPhase = mock(Phase.class);
        var shuffledPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());
        var constructionPhase =
                new DefaultConstructionIndividualStrategy<TestdataListSolution, SimpleScore>(Collections.emptyList(),
                        deterministicPhase, shuffledPhase, localSearchPhase, null, individualBuilder);

        // First call
        var generatedIndividual = constructionPhase.apply(stepScope);
        assertThat(generatedIndividual).isNotNull();
        verify(deterministicPhase).solve(solverScope);
        verify(shuffledPhase, never()).solve(any());

        // Second call
        var bestIndividual = mock(Individual.class);
        var population = stepScope.getPhaseScope().getPopulation();
        doReturn(bestIndividual).when(population).getBestIndividual();
        constructionPhase.apply(stepScope);
        verify(deterministicPhase).solve(solverScope);
        verify(shuffledPhase).solve(solverScope);
    }

    @Test
    void applyWithPhaseCommands() {
        var deterministicPhase = mock(Phase.class);
        var shuffledPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());
        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var command = mock(PhaseCommand.class);
        var constructionPhase = new DefaultConstructionIndividualStrategy<TestdataListSolution, SimpleScore>(List.of(command),
                deterministicPhase, shuffledPhase, localSearchPhase, null, individualBuilder);

        when(stepScope.getMoveDirector()).thenReturn(mock());
        when(stepScope.getWorkingRandom()).thenReturn(mock());
        constructionPhase.apply(stepScope);

        var inOrder = Mockito.inOrder(command, deterministicPhase);
        inOrder.verify(command).changeWorkingSolution(any());
        inOrder.verify(deterministicPhase).solve(solverScope);
    }

    @Test
    void applyWithRefinement() {
        var deterministicPhase = mock(Phase.class);
        var shuffledPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var refinementPhase = mock(Phase.class);
        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());
        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var command = mock(PhaseCommand.class);
        var constructionPhase = new DefaultConstructionIndividualStrategy<TestdataListSolution, SimpleScore>(List.of(command),
                deterministicPhase, shuffledPhase, localSearchPhase, refinementPhase, individualBuilder);

        when(stepScope.getMoveDirector()).thenReturn(mock());
        when(stepScope.getWorkingRandom()).thenReturn(mock());
        constructionPhase.apply(stepScope);

        var inOrder = Mockito.inOrder(command, deterministicPhase, refinementPhase);
        inOrder.verify(command).changeWorkingSolution(any());
        inOrder.verify(deterministicPhase).solve(solverScope);
        inOrder.verify(refinementPhase).solve(solverScope);
    }
}
