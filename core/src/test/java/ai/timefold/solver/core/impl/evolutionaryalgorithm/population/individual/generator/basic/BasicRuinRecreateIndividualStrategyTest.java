package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.basic;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
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
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BasicRuinRecreateIndividualStrategyTest {

    @SuppressWarnings("unchecked")
    private EvolutionaryAlgorithmStepScope<TestdataMultiVarSolution> prepareStepScope() {
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(EvolutionaryAlgorithmPhaseScope.class);
        doReturn(solverScope).when(phaseScope).getSolverScope();
        var solutionDescriptor = TestdataMultiVarSolution.buildSolutionDescriptor();
        var heuristicConfigPolicy =
                new HeuristicConfigPolicy.Builder<TestdataMultiVarSolution>().withSolutionDescriptor(solutionDescriptor)
                        .build();
        var termination = (PhaseTermination<TestdataMultiVarSolution>) TerminationFactory
                .<TestdataMultiVarSolution> create(new TerminationConfig().withStepCountLimit(1))
                .buildTermination(heuristicConfigPolicy);
        doReturn(termination).when(phaseScope).getTermination();
        var stepScope = mock(EvolutionaryAlgorithmStepScope.class);
        doReturn(phaseScope).when(stepScope).getPhaseScope();
        var population = mock(Population.class);
        doReturn(population).when(phaseScope).getPopulation();
        var scoreDirector = mockScoreDirector(solutionDescriptor);
        var problem = TestdataMultiVarSolution.generateUninitializedSolution(1, 1);
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
        var shuffledPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var solutionStateManager = mock(SolutionStateManager.class);
        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());

        var strategy =
                new BasicRuinRecreateIndividualStrategy<TestdataMultiVarSolution, SimpleScore, SolutionState<TestdataMultiVarSolution, SimpleScore>>(
                        Collections.emptyList(), deterministicPhase, shuffledPhase, localSearchPhase, null,
                        solutionStateManager, individualBuilder, 0.95);

        var generatedIndividual = strategy.apply(stepScope);

        assertThat(generatedIndividual).isNotNull();
        verify(deterministicPhase).solve(solverScope);
        verify(localSearchPhase).solve(solverScope);
        verify(shuffledPhase, never()).solve(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyWithBestIndividual() {
        var primaryValue = new TestdataValue("primary");
        var secondaryValue = new TestdataValue("secondary");
        var entity = new TestdataMultiVarEntity("e1");

        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var scoreDirector = (InnerScoreDirector<TestdataMultiVarSolution, SimpleScore>) solverScope.getScoreDirector();

        doReturn(entity).when(scoreDirector).lookUpWorkingObject(entity);
        doReturn(primaryValue).when(scoreDirector).lookUpWorkingObject(primaryValue);
        doReturn(secondaryValue).when(scoreDirector).lookUpWorkingObject(secondaryValue);
        doNothing().when(scoreDirector).executeMove(any());

        var workingRandom = mock(Random.class);
        when(workingRandom.nextInt(2)).thenReturn(0, 1);
        doReturn(workingRandom).when(solverScope).getWorkingRandom();

        var deterministicPhase = mock(Phase.class);
        var shuffledPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var solutionStateManager = mock(SolutionStateManager.class);
        var solutionState = mock(SolutionState.class);
        var bestIndividual = mock(Individual.class);
        doReturn(new ChromosomeEntry[] {
                new ChromosomeEntry(entity, primaryValue, 0),
                new ChromosomeEntry(entity, secondaryValue, 1)
        }).when(bestIndividual).getChromosome();
        doReturn(2).when(bestIndividual).size();
        doReturn(solutionState).when(solutionStateManager).saveSolutionState(scoreDirector, bestIndividual);

        var population = stepScope.getPhaseScope().getPopulation();
        doReturn(bestIndividual).when(population).getBestIndividual();

        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());

        var strategy =
                new BasicRuinRecreateIndividualStrategy<TestdataMultiVarSolution, SimpleScore, SolutionState<TestdataMultiVarSolution, SimpleScore>>(
                        Collections.emptyList(), deterministicPhase, shuffledPhase, localSearchPhase, null,
                        solutionStateManager, individualBuilder, 0.95);

        var generatedIndividual = strategy.apply(stepScope);

        assertThat(generatedIndividual).isNotNull();
        verify(deterministicPhase, never()).solve(any());
        verify(solutionStateManager).saveSolutionState(scoreDirector, bestIndividual);
        verify(solutionStateManager).restoreSolutionState(any(), any());
        verify(shuffledPhase).solve(solverScope);
        verify(localSearchPhase).solve(solverScope);
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyWithPhaseCommands() {
        var stepScope = prepareStepScope();
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        var deterministicPhase = mock(Phase.class);
        var shuffledPhase = mock(Phase.class);
        var localSearchPhase = mock(Phase.class);
        var solutionStateManager = mock(SolutionStateManager.class);
        var individualBuilder = mock(IndividualBuilder.class);
        var individual = mock(Individual.class);
        doReturn(individual).when(individualBuilder).build(any(), any(), any(), any(), any());

        when(stepScope.getMoveDirector()).thenReturn(mock());
        when(stepScope.getWorkingRandom()).thenReturn(mock());

        var command = mock(PhaseCommand.class);
        var strategy =
                new BasicRuinRecreateIndividualStrategy<TestdataMultiVarSolution, SimpleScore, SolutionState<TestdataMultiVarSolution, SimpleScore>>(
                        List.of(command), deterministicPhase, shuffledPhase, localSearchPhase, null, solutionStateManager,
                        individualBuilder, 0.95);

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
        var shuffledPhase = mock(Phase.class);
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
                new BasicRuinRecreateIndividualStrategy<TestdataMultiVarSolution, SimpleScore, SolutionState<TestdataMultiVarSolution, SimpleScore>>(
                        List.of(command), deterministicPhase, shuffledPhase, localSearchPhase, refinementPhase,
                        solutionStateManager, individualBuilder, 0.95);

        strategy.apply(stepScope);

        var inOrder = Mockito.inOrder(command, deterministicPhase, refinementPhase);
        inOrder.verify(command).changeWorkingSolution(any());
        inOrder.verify(deterministicPhase).solve(solverScope);
        inOrder.verify(refinementPhase).solve(solverScope);
    }
}
