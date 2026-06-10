package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.list;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListRuinRecreateIndividualStrategyTest {

    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    void applyWithNoBestIndividual() {
        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var deterministicPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var solutionStateManager = mock(SolutionStateManager.class);
        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());

        var strategy =
                new ListRuinRecreateIndividualStrategy<TestdataListSolution, SimpleScore, SolutionState<TestdataListSolution, SimpleScore>>(
                        Collections.emptyList(), deterministicPhase, localSearchPhase, null, solutionStateManager,
                        individualBuilder, 0.95);

        var generatedIndividual = strategy.apply(stepScope);

        assertThat(generatedIndividual).isNotNull();
        verify(deterministicPhase).solve(solverScope);
        verify(localSearchPhase).solve(solverScope);
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyWithBestIndividual() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var a = new TestdataListEntity("a");

        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var scoreDirector = (InnerScoreDirector<TestdataListSolution, SimpleScore>) solverScope.getScoreDirector();

        var problem = new TestdataListSolution();
        problem.setValueList(new ArrayList<>(List.of(v1, v2)));
        problem.setEntityList(List.of(a));
        a.setValueList(new ArrayList<>(List.of(v1, v2)));
        scoreDirector.setWorkingSolution(problem);

        doReturn(v1).when(scoreDirector).lookUpWorkingObject(v1);
        doReturn(v2).when(scoreDirector).lookUpWorkingObject(v2);
        doReturn(a).when(scoreDirector).lookUpWorkingObject(a);
        doNothing().when(scoreDirector).executeMove(any());

        // Return distinct indices to avoid retry in generateIndexes
        var workingRandom = mock(Random.class);
        when(workingRandom.nextInt(anyInt())).thenReturn(0, 1);
        doReturn(workingRandom).when(solverScope).getWorkingRandom();

        var deterministicPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var solutionStateManager = mock(SolutionStateManager.class);
        var solutionState = mock(SolutionState.class);
        var bestIndividual = mock(Individual.class);
        doReturn(new ChromosomeEntry[] {
                new ChromosomeEntry(a, v1, 0),
                new ChromosomeEntry(a, v2, 1)
        }).when(bestIndividual).getChromosome();
        doReturn(2).when(bestIndividual).size();
        doReturn(solutionState).when(solutionStateManager).saveSolutionState(scoreDirector, bestIndividual);
        doReturn(bestIndividual).when(stepScope).getBestIndividual();

        var population = stepScope.getPhaseScope().getPopulation();
        doReturn(bestIndividual).when(population).getBestIndividual();

        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());

        var strategy =
                new ListRuinRecreateIndividualStrategy<TestdataListSolution, SimpleScore, SolutionState<TestdataListSolution, SimpleScore>>(
                        Collections.emptyList(), deterministicPhase, localSearchPhase, null, solutionStateManager,
                        individualBuilder, 0.95);

        var generatedIndividual = strategy.apply(stepScope);

        assertThat(generatedIndividual).isNotNull();
        // Ruin-recreate path: deterministic phase is skipped
        verify(deterministicPhase, never()).solve(any());
        verify(localSearchPhase).solve(solverScope);
        verify(solutionStateManager).saveSolutionState(scoreDirector, bestIndividual);
        verify(solutionStateManager).restoreSolutionState(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyWithPhaseCommands() {
        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var deterministicPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var solutionStateManager = mock(SolutionStateManager.class);
        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());

        when(stepScope.getMoveDirector()).thenReturn(mock());
        when(stepScope.getWorkingRandom()).thenReturn(mock());

        var command = mock(PhaseCommand.class);
        var strategy =
                new ListRuinRecreateIndividualStrategy<TestdataListSolution, SimpleScore, SolutionState<TestdataListSolution, SimpleScore>>(
                        List.of(command), deterministicPhase, localSearchPhase, null, solutionStateManager, individualBuilder,
                        0.95);

        strategy.apply(stepScope);

        var inOrder = Mockito.inOrder(command, deterministicPhase);
        inOrder.verify(command).changeWorkingSolution(any());
        inOrder.verify(deterministicPhase).solve(solverScope);
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyWithRefinement() {
        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var deterministicPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var refinementPhase = mock(Phase.class);
        var solutionStateManager = mock(SolutionStateManager.class);
        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());

        when(stepScope.getMoveDirector()).thenReturn(mock());
        when(stepScope.getWorkingRandom()).thenReturn(mock());

        var command = mock(PhaseCommand.class);
        var strategy =
                new ListRuinRecreateIndividualStrategy<TestdataListSolution, SimpleScore, SolutionState<TestdataListSolution, SimpleScore>>(
                        List.of(command), deterministicPhase, localSearchPhase, refinementPhase, solutionStateManager,
                        individualBuilder, 0.95);

        strategy.apply(stepScope);

        var inOrder = Mockito.inOrder(command, deterministicPhase, refinementPhase);
        inOrder.verify(command).changeWorkingSolution(any());
        inOrder.verify(deterministicPhase).solve(solverScope);
        inOrder.verify(refinementPhase).solve(solverScope);
    }
}
