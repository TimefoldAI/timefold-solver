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

class ListRXCrossoverTest {

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

    private static EvolutionaryAlgorithmPhaseScope<TestdataListSolution> buildPhaseScope(
            InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector, RandomGenerator random) {
        var phaseScope = mock(EvolutionaryAlgorithmPhaseScope.class);
        doReturn(scoreDirector).when(phaseScope).getScoreDirector();
        var phaseTermination = mock(MockablePhaseTermination.class);
        doReturn(phaseTermination).when(phaseScope).getTermination();
        doReturn(false).when(phaseTermination).isPhaseTerminated(phaseScope);
        doReturn(random).when(phaseScope).getWorkingRandom();
        var solverScope = mock(SolverScope.class);
        doReturn(solverScope).when(phaseScope).getSolverScope();
        doReturn(Clock.systemDefaultZone()).when(solverScope).getClock();
        doReturn(scoreDirector.getWorkingSolution()).when(solverScope).getBestSolution();
        doReturn(InnerScore.fullyAssigned(SimpleScore.ZERO)).when(solverScope).getBestScore();
        var solver = mock(AbstractSolver.class);
        doReturn(solver).when(solverScope).getSolver();
        doReturn(scoreDirector).when(solverScope).getScoreDirector();
        return phaseScope;
    }

    /**
     * All entities inherit from P1 (inheritanceRate=1.0 → always picks P1).
     * Phase 2 finds nothing to place because every value was claimed in Phase 1.
     * <p>
     * P1: a=[v1..v5], b=[v6..v10], c=[]
     * P2: a=[v6..v10], b=[v1..v5], c=[]
     */
    @Test
    void crossoverOneEntityFirstParent() {
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

        var uninitializedSolution = new TestdataListSolution();
        uninitializedSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)));
        uninitializedSolution.setEntityList(new ArrayList<>(List.of(
                new TestdataListEntity("a"),
                new TestdataListEntity("b"),
                new TestdataListEntity("c"))));
        SolutionManager.updateShadowVariables(uninitializedSolution);

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
        var p1a = new TestdataListEntity("a", p1v1, p1v2, p1v3, p1v4, p1v5);
        var p1b = new TestdataListEntity("b", p1v6, p1v7, p1v8, p1v9, p1v10);
        var p1c = new TestdataListEntity("c");
        var firstParent = new TestdataListSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10)));
        firstParent.setEntityList(new ArrayList<>(List.of(p1a, p1b, p1c)));
        SolutionManager.updateShadowVariables(firstParent);

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
        var p2a = new TestdataListEntity("a", p2v6, p2v7, p2v8, p2v9, p2v10);
        var p2b = new TestdataListEntity("b", p2v1, p2v2, p2v3, p2v4, p2v5);
        var p2c = new TestdataListEntity("c");
        var secondParent = new TestdataListSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2v1, p2v2, p2v3, p2v4, p2v5, p2v6, p2v7, p2v8, p2v9, p2v10)));
        secondParent.setEntityList(new ArrayList<>(List.of(p2a, p2b, p2c)));
        SolutionManager.updateShadowVariables(secondParent);

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(uninitializedSolution);
        scoreDirector.calculateScore();
        var random = mock(RandomGenerator.class);
        var phaseScope = buildPhaseScope(scoreDirector, random);

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

        var context = new CrossoverContext<>(phaseScope, firstIndividual, secondIndividual);
        var localSearchPhase = mock(Phase.class);
        var result = new ListRXCrossover<TestdataListSolution, SimpleScore>(
                localSearchPhase, null, 1.0, false).apply(context);
        var offspring = result.solution();

        // inheritanceRate=1.0 → all entities pick P1; Phase 1 appends in order; Phase 2 finds nothing
        assertThat(offspring.getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2", "v3", "v4", "v5");
        assertThat(offspring.getEntityList().get(1).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v6", "v7", "v8", "v9", "v10");
        assertThat(offspring.getEntityList().get(2).getValueList()).isEmpty();
    }

    /**
     * All entities inherit from P2 (inheritanceRate=0 → always picks P2).
     * Phase 2 finds nothing to place because every value was claimed in Phase 1.
     * <p>
     * P1: a=[v1..v5], b=[v6..v10], c=[]
     * P2: a=[v6..v10], b=[v1..v5], c=[]
     */
    @Test
    void crossoverOneEntitySecondParent() {
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

        var uninitializedSolution = new TestdataListSolution();
        uninitializedSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)));
        uninitializedSolution.setEntityList(new ArrayList<>(List.of(
                new TestdataListEntity("a"),
                new TestdataListEntity("b"),
                new TestdataListEntity("c"))));
        SolutionManager.updateShadowVariables(uninitializedSolution);

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
        var p1a = new TestdataListEntity("a", p1v1, p1v2, p1v3, p1v4, p1v5);
        var p1b = new TestdataListEntity("b", p1v6, p1v7, p1v8, p1v9, p1v10);
        var p1c = new TestdataListEntity("c");
        var firstParent = new TestdataListSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10)));
        firstParent.setEntityList(new ArrayList<>(List.of(p1a, p1b, p1c)));
        SolutionManager.updateShadowVariables(firstParent);

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
        var p2a = new TestdataListEntity("a", p2v6, p2v7, p2v8, p2v9, p2v10);
        var p2b = new TestdataListEntity("b", p2v1, p2v2, p2v3, p2v4, p2v5);
        var p2c = new TestdataListEntity("c");
        var secondParent = new TestdataListSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2v1, p2v2, p2v3, p2v4, p2v5, p2v6, p2v7, p2v8, p2v9, p2v10)));
        secondParent.setEntityList(new ArrayList<>(List.of(p2a, p2b, p2c)));
        SolutionManager.updateShadowVariables(secondParent);

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(uninitializedSolution);
        scoreDirector.calculateScore();
        var random = mock(RandomGenerator.class);
        var phaseScope = buildPhaseScope(scoreDirector, random);

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

        var context = new CrossoverContext<>(phaseScope, firstIndividual, secondIndividual);
        var localSearchPhase = mock(Phase.class);
        var result = new ListRXCrossover<TestdataListSolution, SimpleScore>(
                localSearchPhase, null, 0, false).apply(context);
        var offspring = result.solution();

        // inheritanceRate=0 → all entities pick P2; Phase 1 appends in order; Phase 2 finds nothing
        assertThat(offspring.getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v6", "v7", "v8", "v9", "v10");
        assertThat(offspring.getEntityList().get(1).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2", "v3", "v4", "v5");
        assertThat(offspring.getEntityList().get(2).getValueList()).isEmpty();
    }

    /**
     * All entities inherit from P2 (inheritanceRate=0 → always picks P2).
     * Phase 2 finds nothing to place because every value was claimed in Phase 1.
     * <p>
     * P1: a=[v1,v2,v3], b=[v4,v5,v6,v7], c=[v8,v9,v10]
     * P2: a=[v8,v9,v10,v4], b=[v1,v2,v3,v5], c=[v6,v7]
     * <p>
     * Phase 1 (all entities → P2):
     * - a gets P2's [v8,v9,v10,v4] → appended in order → a=[v8,v9,v10,v4]
     * - b gets P2's [v1,v2,v3,v5] → appended in order → b=[v1,v2,v3,v5]
     * - c gets P2's [v6,v7] → appended in order → c=[v6,v7]
     * <p>
     * Phase 2: all values already assigned → nothing placed
     */
    @Test
    void crossoverTwoEntities() {
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

        var uninitializedSolution = new TestdataListSolution();
        uninitializedSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)));
        uninitializedSolution.setEntityList(new ArrayList<>(List.of(
                new TestdataListEntity("a"),
                new TestdataListEntity("b"),
                new TestdataListEntity("c"))));
        SolutionManager.updateShadowVariables(uninitializedSolution);

        // P1: a=[v1,v2,v3], b=[v4,v5,v6,v7], c=[v8,v9,v10]
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
        var p1a = new TestdataListEntity("a", p1v1, p1v2, p1v3);
        var p1b = new TestdataListEntity("b", p1v4, p1v5, p1v6, p1v7);
        var p1c = new TestdataListEntity("c", p1v8, p1v9, p1v10);
        var firstParent = new TestdataListSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10)));
        firstParent.setEntityList(new ArrayList<>(List.of(p1a, p1b, p1c)));
        SolutionManager.updateShadowVariables(firstParent);

        // P2: a=[v8,v9,v10,v4], b=[v1,v2,v3,v5], c=[v6,v7]
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
        var p2a = new TestdataListEntity("a", p2v8, p2v9, p2v10, p2v4);
        var p2b = new TestdataListEntity("b", p2v1, p2v2, p2v3, p2v5);
        var p2c = new TestdataListEntity("c", p2v6, p2v7);
        var secondParent = new TestdataListSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2v1, p2v2, p2v3, p2v4, p2v5, p2v6, p2v7, p2v8, p2v9, p2v10)));
        secondParent.setEntityList(new ArrayList<>(List.of(p2a, p2b, p2c)));
        SolutionManager.updateShadowVariables(secondParent);

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(uninitializedSolution);
        scoreDirector.calculateScore();
        var random = mock(RandomGenerator.class);
        var phaseScope = buildPhaseScope(scoreDirector, random);

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

        var context = new CrossoverContext<>(phaseScope, firstIndividual, secondIndividual);
        var localSearchPhase = mock(Phase.class);
        var result = new ListRXCrossover<TestdataListSolution, SimpleScore>(
                localSearchPhase, null, 0, false).apply(context);
        var offspring = result.solution();

        assertThat(offspring.getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v8", "v9", "v10", "v4");
        assertThat(offspring.getEntityList().get(1).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2", "v3", "v5");
        assertThat(offspring.getEntityList().get(2).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v6", "v7");
    }

    /**
     * Mixed inheritance: a=P1, b=P2, c=P1 (inheritanceRate=0.5, nextDouble returns 0.0, 0.9, 0.0).
     * <p>
     * P1: a=[v1,v2,v3], b=[v4,v5,v6,v7], c=[v8,v9,v10]
     * P2: a=[v8,v9,v10,v4], b=[v1,v2,v3,v5], c=[v6,v7]
     * <p>
     * Phase 1 (a→P1, b→P2, c→P1):
     * - a gets P1's [v1,v2,v3] → appended in order → a=[v1,v2,v3]
     * - b gets P2's [v1,v2,v3,v5] → v1,v2,v3 already assigned → only v5 placed → b=[v5]
     * - c gets P1's [v8,v9,v10] → appended in order → c=[v8,v9,v10]
     * <p>
     * Phase 2 (P2 order: v8,v9,v10,v4 from a; v1,v2,v3,v5 from b; v6,v7 from c):
     * - v8,v9,v10: already assigned → skip
     * - v4: unassigned → best fit across entities → entity a at pos 0 → a=[v4,v1,v2,v3]
     * - v1,v2,v3,v5: already assigned → skip
     * - v6: unassigned → entity a at pos 0 → a=[v6,v4,v1,v2,v3]
     * - v7: unassigned → entity a at pos 0 → a=[v7,v6,v4,v1,v2,v3]
     */
    @Test
    void crossoverTwoEntitiesWithInheritanceRate() {
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

        var uninitializedSolution = new TestdataListSolution();
        uninitializedSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)));
        uninitializedSolution.setEntityList(new ArrayList<>(List.of(
                new TestdataListEntity("a"),
                new TestdataListEntity("b"),
                new TestdataListEntity("c"))));
        SolutionManager.updateShadowVariables(uninitializedSolution);

        // P1: a=[v1,v2,v3], b=[v4,v5,v6,v7], c=[v8,v9,v10]
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
        var p1a = new TestdataListEntity("a", p1v1, p1v2, p1v3);
        var p1b = new TestdataListEntity("b", p1v4, p1v5, p1v6, p1v7);
        var p1c = new TestdataListEntity("c", p1v8, p1v9, p1v10);
        var firstParent = new TestdataListSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1v1, p1v2, p1v3, p1v4, p1v5, p1v6, p1v7, p1v8, p1v9, p1v10)));
        firstParent.setEntityList(new ArrayList<>(List.of(p1a, p1b, p1c)));
        SolutionManager.updateShadowVariables(firstParent);

        // P2: a=[v8,v9,v10,v4], b=[v1,v2,v3,v5], c=[v6,v7]
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
        var p2a = new TestdataListEntity("a", p2v8, p2v9, p2v10, p2v4);
        var p2b = new TestdataListEntity("b", p2v1, p2v2, p2v3, p2v5);
        var p2c = new TestdataListEntity("c", p2v6, p2v7);
        var secondParent = new TestdataListSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2v1, p2v2, p2v3, p2v4, p2v5, p2v6, p2v7, p2v8, p2v9, p2v10)));
        secondParent.setEntityList(new ArrayList<>(List.of(p2a, p2b, p2c)));
        SolutionManager.updateShadowVariables(secondParent);

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(uninitializedSolution);
        scoreDirector.calculateScore();
        var random = mock(RandomGenerator.class);
        // 0.0 < 0.5 → P1 for a; 0.9 < 0.5 → false → P2 for b; 0.0 < 0.5 → P1 for c
        when(random.nextDouble()).thenReturn(0.0, 0.9, 0.0);
        var phaseScope = buildPhaseScope(scoreDirector, random);

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

        var context = new CrossoverContext<>(phaseScope, firstIndividual, secondIndividual);
        var localSearchPhase = mock(Phase.class);
        var result = new ListRXCrossover<TestdataListSolution, SimpleScore>(
                localSearchPhase, null, 0.5, false).apply(context);
        var offspring = result.solution();

        // a: Phase 1 [v1,v2,v3] from P1; Phase 2 prepends v4, v6, v7 → [v7,v6,v4,v1,v2,v3]
        assertThat(offspring.getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v7", "v6", "v4", "v1", "v2", "v3");
        // b: Phase 1 only v5 from P2 (v1,v2,v3 already taken) → [v5]
        assertThat(offspring.getEntityList().get(1).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v5");
        // c: Phase 1 [v8,v9,v10] from P1 → [v8,v9,v10]
        assertThat(offspring.getEntityList().get(2).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v8", "v9", "v10");
    }
}
