package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.MockablePhaseTermination;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

class ListOXCrossoverTest {

    private static InnerScoreDirector<TestdataListSolution, SimpleScore> buildScoreDirector() {
        var factory = new EasyScoreDirectorFactory<>(TestdataListSolution.buildSolutionDescriptor(),
                solution -> SimpleScore.of(0), EnvironmentMode.PHASE_ASSERT);
        factory.setInitializingScoreTrend(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1));
        var delegate = factory.createScoreDirectorBuilder()
                .withLookUpEnabled(true)
                .build();
        return mock(InnerScoreDirector.class, AdditionalAnswers.delegatesTo(delegate));
    }

    @Test
    void crossoverOneEntity() {
        // Uninitialized solution
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var v4 = new TestdataListValue("v4");
        var v5 = new TestdataListValue("v5");
        var v6 = new TestdataListValue("v6");
        var v7 = new TestdataListValue("v7");
        var v8 = new TestdataListValue("v8");
        var v9 = new TestdataListValue("v9");
        var v10 = new TestdataListValue("v10");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");
        var c = new TestdataListEntity("c");

        var uninitializedSolution = new TestdataListSolution();
        uninitializedSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)));
        uninitializedSolution.setEntityList(new ArrayList<>(List.of(a, b, c)));
        SolutionManager.updateShadowVariables(uninitializedSolution);

        // First parent: a=[v1..v10], b=[], c=[]
        var p1v1 = new TestdataListValue("v1");
        var p1v2 = new TestdataListValue("v2");
        var p1v3 = new TestdataListValue("v3");
        var p1v4 = new TestdataListValue("v4");
        var p1v5 = new TestdataListValue("v5");
        var p1v6 = new TestdataListValue("v6");
        var p1v7 = new TestdataListValue("v7");
        var p1v8 = new TestdataListValue("v8");
        var p1v9 = new TestdataListValue("v9");
        var p1v10 = new TestdataListValue("v10");

        var a1 = new TestdataListEntity("a", p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10);
        var b1 = new TestdataListEntity("b");
        var c1 = new TestdataListEntity("c");

        var firstParent = new TestdataListSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10)));
        firstParent.setEntityList(new ArrayList<>(List.of(a1, b1, c1)));
        SolutionManager.updateShadowVariables(firstParent);

        // Second parent: a=[v10,v9,v8,v7,v6,v5,v4,v3,v2,v1], b=[], c=[]
        var p2v1 = new TestdataListValue("v1");
        var p2v2 = new TestdataListValue("v2");
        var p2v3 = new TestdataListValue("v3");
        var p2v4 = new TestdataListValue("v4");
        var p2v5 = new TestdataListValue("v5");
        var p2v6 = new TestdataListValue("v6");
        var p2v7 = new TestdataListValue("v7");
        var p2v8 = new TestdataListValue("v8");
        var p2v9 = new TestdataListValue("v9");
        var p2v10 = new TestdataListValue("v10");

        var a2 = new TestdataListEntity("a", p2v10, p2v9, p2v8, p2v7, p2v6, p2v5, p2v4, p2v3, p2v2, p2v1);
        var b2 = new TestdataListEntity("b");
        var c2 = new TestdataListEntity("c");

        var secondParent = new TestdataListSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2v1, p2v2, p2v3, p2v4, p2v5, p2v6, p2v7, p2v8, p2v9, p2v10)));
        secondParent.setEntityList(new ArrayList<>(List.of(a2, b2, c2)));
        SolutionManager.updateShadowVariables(secondParent);

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(uninitializedSolution);
        scoreDirector.calculateScore();
        var phaseScope = mock(EvolutionaryAlgorithmPhaseScope.class);
        doReturn(scoreDirector).when(phaseScope).getScoreDirector();
        var phaseTermination = mock(MockablePhaseTermination.class);
        doReturn(phaseTermination).when(phaseScope).getTermination();
        doReturn(false).when(phaseTermination).isPhaseTerminated(phaseScope);
        var random = mock(RandomGenerator.class);
        doReturn(random).when(phaseScope).getWorkingRandom();
        var solverScope = mock(SolverScope.class);
        doReturn(scoreDirector).when(solverScope).getScoreDirector();
        doReturn(solverScope).when(phaseScope).getSolverScope();
        doReturn(Clock.systemDefaultZone()).when(solverScope).getClock();
        doReturn(scoreDirector.getWorkingSolution()).when(solverScope).getBestSolution();
        doReturn(InnerScore.fullyAssigned(SimpleScore.ZERO)).when(solverScope).getBestScore();
        var solver = mock(AbstractSolver.class);
        doReturn(solver).when(solverScope).getSolver();

        var firstIndividual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        when(firstIndividual.size()).thenReturn(10);
        when(firstIndividual.getSolution()).thenReturn(firstParent);
        when(firstIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(firstIndividual.getChromosome()).thenReturn(firstParent.getEntityList().stream()
                .flatMap(e -> e.getValueList().stream().map(v -> new ChromosomeEntry(e, v, 0)))
                .toArray(ChromosomeEntry[]::new));

        var secondIndividual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        when(secondIndividual.size()).thenReturn(10);
        when(secondIndividual.getSolution()).thenReturn(secondParent);
        when(secondIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(secondIndividual.getChromosome()).thenReturn(secondParent.getEntityList().stream()
                .flatMap(e -> e.getValueList().stream().map(v -> new ChromosomeEntry(e, v, 0)))
                .toArray(ChromosomeEntry[]::new));

        // Cut [2, 5] → both within entity a → fixIndex snaps to [0, 10] → all P1 values
        when(random.nextInt(10)).thenReturn(2, 5);
        var localSearchPhase = mock(Phase.class);
        var context = new CrossoverContext<TestdataListSolution, SimpleScore>(phaseScope, firstIndividual, secondIndividual);
        // No inheritance rate
        var result =
                new ListOXCrossover<TestdataListSolution, SimpleScore>(localSearchPhase, null, 0, false, random).apply(context);
        var offspring = result.solution();

        // a inherit all P1 values with the same position from the parent
        assertThat(offspring.getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9", "v10");
        assertThat(offspring.getEntityList().get(1).getValueList()).isEmpty();
        assertThat(offspring.getEntityList().get(2).getValueList()).isEmpty();
    }

    @Test
    void crossoverTwoEntities() {
        // Uninitialized solution
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var v4 = new TestdataListValue("v4");
        var v5 = new TestdataListValue("v5");
        var v6 = new TestdataListValue("v6");
        var v7 = new TestdataListValue("v7");
        var v8 = new TestdataListValue("v8");
        var v9 = new TestdataListValue("v9");
        var v10 = new TestdataListValue("v10");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");
        var c = new TestdataListEntity("c");

        var uninitializedSolution = new TestdataListSolution();
        uninitializedSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)));
        uninitializedSolution.setEntityList(new ArrayList<>(List.of(a, b, c)));
        SolutionManager.updateShadowVariables(uninitializedSolution);

        // First parent: a=[v1..v5], b=[v6..v10], c=[]
        var p1v1 = new TestdataListValue("v1");
        var p1v2 = new TestdataListValue("v2");
        var p1v3 = new TestdataListValue("v3");
        var p1v4 = new TestdataListValue("v4");
        var p1v5 = new TestdataListValue("v5");
        var p1v6 = new TestdataListValue("v6");
        var p1v7 = new TestdataListValue("v7");
        var p1v8 = new TestdataListValue("v8");
        var p1v9 = new TestdataListValue("v9");
        var p1v10 = new TestdataListValue("v10");

        var a1 = new TestdataListEntity("a", p1v1, p1v2, p1v3, p1v4, p1v5);
        var b1 = new TestdataListEntity("b", p1v6, p1v7, p1v8, p1v9, p1v10);
        var c1 = new TestdataListEntity("c");

        var firstParent = new TestdataListSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10)));
        firstParent.setEntityList(new ArrayList<>(List.of(a1, b1, c1)));
        SolutionManager.updateShadowVariables(firstParent);

        // Second parent: a=[v6..v10], b=[v5,v4,v3,v2,v1], c=[]
        var p2v1 = new TestdataListValue("v1");
        var p2v2 = new TestdataListValue("v2");
        var p2v3 = new TestdataListValue("v3");
        var p2v4 = new TestdataListValue("v4");
        var p2v5 = new TestdataListValue("v5");
        var p2v6 = new TestdataListValue("v6");
        var p2v7 = new TestdataListValue("v7");
        var p2v8 = new TestdataListValue("v8");
        var p2v9 = new TestdataListValue("v9");
        var p2v10 = new TestdataListValue("v10");

        var a2 = new TestdataListEntity("a", p2v6, p2v7, p2v8, p2v9, p2v10);
        var b2 = new TestdataListEntity("b", p2v5, p2v4, p2v3, p2v2, p2v1);
        var c2 = new TestdataListEntity("c");

        var secondParent = new TestdataListSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2v1, p2v2, p2v3, p2v4, p2v5, p2v6, p2v7, p2v8, p2v9, p2v10)));
        secondParent.setEntityList(new ArrayList<>(List.of(a2, b2, c2)));
        SolutionManager.updateShadowVariables(secondParent);

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(uninitializedSolution);
        scoreDirector.calculateScore();
        var phaseScope = mock(EvolutionaryAlgorithmPhaseScope.class);
        doReturn(scoreDirector).when(phaseScope).getScoreDirector();
        var phaseTermination = mock(MockablePhaseTermination.class);
        doReturn(phaseTermination).when(phaseScope).getTermination();
        doReturn(false).when(phaseTermination).isPhaseTerminated(phaseScope);
        var random = mock(RandomGenerator.class);
        doReturn(random).when(phaseScope).getWorkingRandom();
        var solverScope = mock(SolverScope.class);
        doReturn(scoreDirector).when(solverScope).getScoreDirector();
        doReturn(solverScope).when(phaseScope).getSolverScope();
        doReturn(Clock.systemDefaultZone()).when(solverScope).getClock();
        doReturn(scoreDirector.getWorkingSolution()).when(solverScope).getBestSolution();
        doReturn(InnerScore.fullyAssigned(SimpleScore.ZERO)).when(solverScope).getBestScore();
        var solver = mock(AbstractSolver.class);
        doReturn(solver).when(solverScope).getSolver();

        var firstIndividual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        when(firstIndividual.size()).thenReturn(10);
        when(firstIndividual.getSolution()).thenReturn(firstParent);
        when(firstIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(firstIndividual.getChromosome()).thenReturn(firstParent.getEntityList().stream()
                .flatMap(e -> e.getValueList().stream().map(v -> new ChromosomeEntry(e, v, 0)))
                .toArray(ChromosomeEntry[]::new));

        var secondIndividual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        when(secondIndividual.size()).thenReturn(10);
        when(secondIndividual.getSolution()).thenReturn(secondParent);
        when(secondIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(secondIndividual.getChromosome()).thenReturn(secondParent.getEntityList().stream()
                .flatMap(e -> e.getValueList().stream().map(v -> new ChromosomeEntry(e, v, 0)))
                .toArray(ChromosomeEntry[]::new));

        // Cut [3, 7] → start mid-a (snaps to 0), end mid-b (snaps to 10) → all P1
        when(random.nextInt(10)).thenReturn(3, 7);
        var context = new CrossoverContext<TestdataListSolution, SimpleScore>(phaseScope, firstIndividual, secondIndividual);
        var localSearchPhase = mock(Phase.class);
        // No inheritance rate
        var result =
                new ListOXCrossover<TestdataListSolution, SimpleScore>(localSearchPhase, null, 0, false, random)
                        .apply(context);
        var offspring = result.solution();

        // a and b inherit all P1 values with the same position from the parent
        assertThat(offspring.getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2", "v3", "v4", "v5");
        assertThat(offspring.getEntityList().get(1).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v6", "v7", "v8", "v9", "v10");
        assertThat(offspring.getEntityList().get(2).getValueList()).isEmpty();
    }

    @Test
    void crossoverThreeEntities() {
        // Uninitialized solution
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var v4 = new TestdataListValue("v4");
        var v5 = new TestdataListValue("v5");
        var v6 = new TestdataListValue("v6");
        var v7 = new TestdataListValue("v7");
        var v8 = new TestdataListValue("v8");
        var v9 = new TestdataListValue("v9");
        var v10 = new TestdataListValue("v10");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");
        var c = new TestdataListEntity("c");

        var uninitializedSolution = new TestdataListSolution();
        uninitializedSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)));
        uninitializedSolution.setEntityList(new ArrayList<>(List.of(a, b, c)));
        SolutionManager.updateShadowVariables(uninitializedSolution);

        // First parent: a=[v1..v4], b=[v5,v6,v7], c=[v8,v9,v10]
        var p1v1 = new TestdataListValue("v1");
        var p1v2 = new TestdataListValue("v2");
        var p1v3 = new TestdataListValue("v3");
        var p1v4 = new TestdataListValue("v4");
        var p1v5 = new TestdataListValue("v5");
        var p1v6 = new TestdataListValue("v6");
        var p1v7 = new TestdataListValue("v7");
        var p1v8 = new TestdataListValue("v8");
        var p1v9 = new TestdataListValue("v9");
        var p1v10 = new TestdataListValue("v10");

        var a1 = new TestdataListEntity("a", p1v1, p1v2, p1v3, p1v4);
        var b1 = new TestdataListEntity("b", p1v5, p1v6, p1v7);
        var c1 = new TestdataListEntity("c", p1v8, p1v9, p1v10);

        var firstParent = new TestdataListSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10)));
        firstParent.setEntityList(new ArrayList<>(List.of(a1, b1, c1)));
        SolutionManager.updateShadowVariables(firstParent);

        // Second parent: a=[v10,v9,v8], b=[v7,v6,v5,v4], c=[v3,v2,v1]
        var p2v1 = new TestdataListValue("v1");
        var p2v2 = new TestdataListValue("v2");
        var p2v3 = new TestdataListValue("v3");
        var p2v4 = new TestdataListValue("v4");
        var p2v5 = new TestdataListValue("v5");
        var p2v6 = new TestdataListValue("v6");
        var p2v7 = new TestdataListValue("v7");
        var p2v8 = new TestdataListValue("v8");
        var p2v9 = new TestdataListValue("v9");
        var p2v10 = new TestdataListValue("v10");

        var a2 = new TestdataListEntity("a", p2v10, p2v9, p2v8);
        var b2 = new TestdataListEntity("b", p2v7, p2v6, p2v5, p2v4);
        var c2 = new TestdataListEntity("c", p2v3, p2v2, p2v1);

        var secondParent = new TestdataListSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2v1, p2v2, p2v3, p2v4, p2v5, p2v6, p2v7, p2v8, p2v9, p2v10)));
        secondParent.setEntityList(new ArrayList<>(List.of(a2, b2, c2)));
        SolutionManager.updateShadowVariables(secondParent);

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(uninitializedSolution);
        scoreDirector.calculateScore();
        var phaseScope = mock(EvolutionaryAlgorithmPhaseScope.class);
        doReturn(scoreDirector).when(phaseScope).getScoreDirector();
        var phaseTermination = mock(MockablePhaseTermination.class);
        doReturn(phaseTermination).when(phaseScope).getTermination();
        doReturn(false).when(phaseTermination).isPhaseTerminated(phaseScope);
        var random = mock(RandomGenerator.class);
        doReturn(random).when(phaseScope).getWorkingRandom();
        var solverScope = mock(SolverScope.class);
        doReturn(scoreDirector).when(solverScope).getScoreDirector();
        doReturn(solverScope).when(phaseScope).getSolverScope();
        doReturn(Clock.systemDefaultZone()).when(solverScope).getClock();
        doReturn(scoreDirector.getWorkingSolution()).when(solverScope).getBestSolution();
        doReturn(InnerScore.fullyAssigned(SimpleScore.ZERO)).when(solverScope).getBestScore();
        var solver = mock(AbstractSolver.class);
        doReturn(solver).when(solverScope).getSolver();

        var firstIndividual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        when(firstIndividual.size()).thenReturn(10);
        when(firstIndividual.getSolution()).thenReturn(firstParent);
        when(firstIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(firstIndividual.getChromosome()).thenReturn(firstParent.getEntityList().stream()
                .flatMap(e -> e.getValueList().stream().map(v -> new ChromosomeEntry(e, v, 0)))
                .toArray(ChromosomeEntry[]::new));

        var secondIndividual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        when(secondIndividual.size()).thenReturn(10);
        when(secondIndividual.getSolution()).thenReturn(secondParent);
        when(secondIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(secondIndividual.getChromosome()).thenReturn(secondParent.getEntityList().stream()
                .flatMap(e -> e.getValueList().stream().map(v -> new ChromosomeEntry(e, v, 0)))
                .toArray(ChromosomeEntry[]::new));

        // Cut [2, 8] → start mid-a (snaps to 0), end mid-c (snaps to 10) → all P1
        when(random.nextInt(10)).thenReturn(2, 8);
        var context = new CrossoverContext<TestdataListSolution, SimpleScore>(phaseScope, firstIndividual, secondIndividual);
        var localSearchPhase = mock(Phase.class);
        // No inheritance rate
        var result =
                new ListOXCrossover<TestdataListSolution, SimpleScore>(localSearchPhase, null, 0, false, random)
                        .apply(context);
        var offspring = result.solution();

        // a, b and c inherit all P1 values with the same position from the parent
        assertThat(offspring.getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode).containsExactly("v1", "v2", "v3", "v4");
        assertThat(offspring.getEntityList().get(1).getValueList())
                .extracting(TestdataListValue::getCode).containsExactly("v5", "v6", "v7");
        assertThat(offspring.getEntityList().get(2).getValueList())
                .extracting(TestdataListValue::getCode).containsExactly("v8", "v9", "v10");
    }

    @Test
    @SuppressWarnings("unchecked")
    void crossoverThreeEntitiesWithInheritanceRate() {
        // Uninitialized solution
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var v4 = new TestdataListValue("v4");
        var v5 = new TestdataListValue("v5");
        var v6 = new TestdataListValue("v6");
        var v7 = new TestdataListValue("v7");
        var v8 = new TestdataListValue("v8");
        var v9 = new TestdataListValue("v9");
        var v10 = new TestdataListValue("v10");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");
        var c = new TestdataListEntity("c");

        var uninitializedSolution = new TestdataListSolution();
        uninitializedSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)));
        uninitializedSolution.setEntityList(new ArrayList<>(List.of(a, b, c)));
        SolutionManager.updateShadowVariables(uninitializedSolution);

        // First parent: a=[v1..v4], b=[v5,v6,v7], c=[v8,v9,v10]
        var p1v1 = new TestdataListValue("v1");
        var p1v2 = new TestdataListValue("v2");
        var p1v3 = new TestdataListValue("v3");
        var p1v4 = new TestdataListValue("v4");
        var p1v5 = new TestdataListValue("v5");
        var p1v6 = new TestdataListValue("v6");
        var p1v7 = new TestdataListValue("v7");
        var p1v8 = new TestdataListValue("v8");
        var p1v9 = new TestdataListValue("v9");
        var p1v10 = new TestdataListValue("v10");

        var a1 = new TestdataListEntity("a", p1v1, p1v2, p1v3, p1v4);
        var b1 = new TestdataListEntity("b", p1v5, p1v6, p1v7);
        var c1 = new TestdataListEntity("c", p1v8, p1v9, p1v10);

        var firstParent = new TestdataListSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10)));
        firstParent.setEntityList(new ArrayList<>(List.of(a1, b1, c1)));
        SolutionManager.updateShadowVariables(firstParent);

        // Second parent: a=[v10,v9,v8], b=[v7,v6,v5,v4], c=[v3,v2,v1]
        var p2v1 = new TestdataListValue("v1");
        var p2v2 = new TestdataListValue("v2");
        var p2v3 = new TestdataListValue("v3");
        var p2v4 = new TestdataListValue("v4");
        var p2v5 = new TestdataListValue("v5");
        var p2v6 = new TestdataListValue("v6");
        var p2v7 = new TestdataListValue("v7");
        var p2v8 = new TestdataListValue("v8");
        var p2v9 = new TestdataListValue("v9");
        var p2v10 = new TestdataListValue("v10");

        var a2 = new TestdataListEntity("a", p2v10, p2v9, p2v8);
        var b2 = new TestdataListEntity("b", p2v7, p2v6, p2v5, p2v4);
        var c2 = new TestdataListEntity("c", p2v3, p2v2, p2v1);

        var secondParent = new TestdataListSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2v1, p2v2, p2v3, p2v4, p2v5, p2v6, p2v7, p2v8, p2v9, p2v10)));
        secondParent.setEntityList(new ArrayList<>(List.of(a2, b2, c2)));
        SolutionManager.updateShadowVariables(secondParent);

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(uninitializedSolution);
        scoreDirector.calculateScore();
        var phaseScope = mock(EvolutionaryAlgorithmPhaseScope.class);
        doReturn(scoreDirector).when(phaseScope).getScoreDirector();
        var phaseTermination = mock(MockablePhaseTermination.class);
        doReturn(phaseTermination).when(phaseScope).getTermination();
        doReturn(false).when(phaseTermination).isPhaseTerminated(phaseScope);
        var random = mock(RandomGenerator.class);
        doReturn(random).when(phaseScope).getWorkingRandom();
        var solverScope = mock(SolverScope.class);
        doReturn(scoreDirector).when(solverScope).getScoreDirector();
        doReturn(solverScope).when(phaseScope).getSolverScope();
        doReturn(Clock.systemDefaultZone()).when(solverScope).getClock();
        doReturn(scoreDirector.getWorkingSolution()).when(solverScope).getBestSolution();
        doReturn(InnerScore.fullyAssigned(SimpleScore.ZERO)).when(solverScope).getBestScore();
        var solver = mock(AbstractSolver.class);
        doReturn(solver).when(solverScope).getSolver();

        var firstIndividual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        when(firstIndividual.size()).thenReturn(10);
        when(firstIndividual.getSolution()).thenReturn(firstParent);
        when(firstIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(firstIndividual.getChromosome()).thenReturn(firstParent.getEntityList().stream()
                .flatMap(e -> e.getValueList().stream().map(v -> new ChromosomeEntry(e, v, 0)))
                .toArray(ChromosomeEntry[]::new));

        var secondIndividual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        when(secondIndividual.size()).thenReturn(10);
        when(secondIndividual.getSolution()).thenReturn(secondParent);
        when(secondIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(secondIndividual.getChromosome()).thenReturn(secondParent.getEntityList().stream()
                .flatMap(e -> e.getValueList().stream().map(v -> new ChromosomeEntry(e, v, 0)))
                .toArray(ChromosomeEntry[]::new));

        // inheritanceRate=0.5, size=10 → minSize=5, maxStart=6
        // nextInt(0,6)=2 → start=2; minEnd=6, maxEnd=10 → nextInt(6,10)=8 → end=8
        // fixIndex snaps start mid-a to 0, end mid-c to 10 → all P1 values inherited
        when(random.nextInt(0, 6)).thenReturn(2);
        when(random.nextInt(6, 10)).thenReturn(8);
        var context = new CrossoverContext<TestdataListSolution, SimpleScore>(phaseScope, firstIndividual, secondIndividual);
        var localSearchPhase = mock(Phase.class);
        var result =
                new ListOXCrossover<TestdataListSolution, SimpleScore>(localSearchPhase, null, 0.5, false, random)
                        .apply(context);
        var offspring = result.solution();

        // a, b and c inherit all P1 values with the same position from the parent
        assertThat(offspring.getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode).containsExactly("v1", "v2", "v3", "v4");
        assertThat(offspring.getEntityList().get(1).getValueList())
                .extracting(TestdataListValue::getCode).containsExactly("v5", "v6", "v7");
        assertThat(offspring.getEntityList().get(2).getValueList())
                .extracting(TestdataListValue::getCode).containsExactly("v8", "v9", "v10");
    }
}
