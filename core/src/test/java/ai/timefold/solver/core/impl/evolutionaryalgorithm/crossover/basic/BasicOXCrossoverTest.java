package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.SimpleScore;
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
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataOtherValue;

import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

class BasicOXCrossoverTest {

    private static InnerScoreDirector<TestdataSolution, SimpleScore> buildScoreDirector() {
        var factory = new EasyScoreDirectorFactory<>(TestdataSolution.buildSolutionDescriptor(),
                solution -> SimpleScore.of(0), EnvironmentMode.PHASE_ASSERT);
        factory.setInitializingScoreTrend(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1));
        var delegate = factory.createScoreDirectorBuilder()
                .withLookUpEnabled(true)
                .build();
        return mock(InnerScoreDirector.class, AdditionalAnswers.delegatesTo(delegate));
    }

    private static InnerScoreDirector<TestdataMultiVarSolution, SimpleScore> buildMultiVarScoreDirector() {
        var factory = new EasyScoreDirectorFactory<>(TestdataMultiVarSolution.buildSolutionDescriptor(),
                solution -> SimpleScore.of(0), EnvironmentMode.PHASE_ASSERT);
        factory.setInitializingScoreTrend(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1));
        var delegate = factory.createScoreDirectorBuilder()
                .withLookUpEnabled(true)
                .build();
        return mock(InnerScoreDirector.class, AdditionalAnswers.delegatesTo(delegate));
    }

    private static ChromosomeEntry[] buildChromosome(TestdataSolution solution) {
        var varDescriptors = TestdataEntity.buildEntityDescriptor().getBasicVariableDescriptorList();
        var chromosomeList = new ArrayList<ChromosomeEntry>();
        for (var entity : solution.getEntityList()) {
            for (var i = 0; i < varDescriptors.size(); i++) {
                chromosomeList.add(new ChromosomeEntry(entity, varDescriptors.get(i).getValue(entity), i));
            }
        }
        return chromosomeList.toArray(ChromosomeEntry[]::new);
    }

    private static ChromosomeEntry[] buildMultiVarChromosome(TestdataMultiVarSolution solution) {
        var varDescriptors = TestdataMultiVarEntity.buildEntityDescriptor().getBasicVariableDescriptorList();
        var chromosomeList = new ArrayList<ChromosomeEntry>();
        for (var entity : solution.getMultiVarEntityList()) {
            for (var i = 0; i < varDescriptors.size(); i++) {
                chromosomeList.add(new ChromosomeEntry(entity, varDescriptors.get(i).getValue(entity), i));
            }
        }
        return chromosomeList.toArray(ChromosomeEntry[]::new);
    }

    @Test
    @SuppressWarnings("unchecked")
    void crossoverSingleVariable() {
        // Working solution: 3 entities, 1 basic variable each.
        // All values that appear in either parent must be present so lookUpWorkingObject can rebase them.
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var va1 = new TestdataValue("va1");
        var vb1 = new TestdataValue("vb1");
        var vc1 = new TestdataValue("vc1");
        var va2 = new TestdataValue("va2");
        var vb2 = new TestdataValue("vb2");
        var vc2 = new TestdataValue("vc2");

        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);
        var e3 = new TestdataEntity("e3", v3);

        var workingSolution = new TestdataSolution();
        workingSolution.setValueList(new ArrayList<>(List.of(v1, v2, v3, va1, vb1, vc1, va2, vb2, vc2)));
        workingSolution.setEntityList(new ArrayList<>(List.of(e1, e2, e3)));

        // First parent: e1=va1, e2=vb1, e3=vc1
        var p1e1 = new TestdataEntity("e1", va1);
        var p1e2 = new TestdataEntity("e2", vb1);
        var p1e3 = new TestdataEntity("e3", vc1);
        var firstParent = new TestdataSolution();
        firstParent.setValueList(new ArrayList<>(List.of(va1, vb1, vc1)));
        firstParent.setEntityList(new ArrayList<>(List.of(p1e1, p1e2, p1e3)));

        // Second parent: e1=va2, e2=vb2, e3=vc2
        var p2e1 = new TestdataEntity("e1", va2);
        var p2e2 = new TestdataEntity("e2", vb2);
        var p2e3 = new TestdataEntity("e3", vc2);
        var secondParent = new TestdataSolution();
        secondParent.setValueList(new ArrayList<>(List.of(va2, vb2, vc2)));
        secondParent.setEntityList(new ArrayList<>(List.of(p2e1, p2e2, p2e3)));

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(workingSolution);
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
        doReturn(mock(AbstractSolver.class)).when(solverScope).getSolver();

        var firstIndividual = (Individual<TestdataSolution, SimpleScore>) mock(Individual.class);
        when(firstIndividual.size()).thenReturn(3);
        when(firstIndividual.getSolution()).thenReturn(firstParent);
        when(firstIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(firstIndividual.getChromosome()).thenReturn(buildChromosome(firstParent));

        var secondIndividual = (Individual<TestdataSolution, SimpleScore>) mock(Individual.class);
        when(secondIndividual.size()).thenReturn(3);
        when(secondIndividual.getSolution()).thenReturn(secondParent);
        when(secondIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(secondIndividual.getChromosome()).thenReturn(buildChromosome(secondParent));

        // generateIndexes(size=3, rate=0): nextInt(3) → 1, 2 → [1, 2]
        // fixIndex(p1, 1, true): target=e2, chromosome[0].entity()=e1 ≠ e2 → return 1
        // fixIndex(p1, 2, false): target=e3, index=3 → past end → return 3
        // Effective cut: P1 applies [1, 3) = {e2, e3}; P2 applies [0, 1) = {e1}
        when(random.nextInt(3)).thenReturn(1, 2);

        var localSearchPhase = mock(Phase.class);
        var context = new CrossoverContext<TestdataSolution, SimpleScore>(phaseScope, firstIndividual, secondIndividual);
        var result = new BasicOXCrossover<TestdataSolution, SimpleScore>(localSearchPhase, null, 0, random).apply(context);
        var offspring = result.solution();

        assertThat(offspring.getEntityList().get(0).getValue().getCode()).isEqualTo("va2"); // e1 from P2
        assertThat(offspring.getEntityList().get(1).getValue().getCode()).isEqualTo("vb1"); // e2 from P1
        assertThat(offspring.getEntityList().get(2).getValue().getCode()).isEqualTo("vc1"); // e3 from P1
    }

    @Test
    @SuppressWarnings("unchecked")
    void crossoverMultipleVariables() {
        // Working solution: 3 entities, each with 3 basic variables (primary, secondary, tertiary).
        // All values from both parents are in the working solution's value list so lookUpWorkingObject works.
        var p1e1Prim = new TestdataValue("p1e1Prim");
        var p1e1Sec = new TestdataValue("p1e1Sec");
        var p1e2Prim = new TestdataValue("p1e2Prim");
        var p1e2Sec = new TestdataValue("p1e2Sec");
        var p1e3Prim = new TestdataValue("p1e3Prim");
        var p1e3Sec = new TestdataValue("p1e3Sec");
        var p2e1Prim = new TestdataValue("p2e1Prim");
        var p2e1Sec = new TestdataValue("p2e1Sec");
        var p2e1Ter = new TestdataOtherValue("p2e1Ter");
        var p2e2Prim = new TestdataValue("p2e2Prim");
        var p2e2Sec = new TestdataValue("p2e2Sec");
        var p2e3Prim = new TestdataValue("p2e3Prim");
        var p2e3Sec = new TestdataValue("p2e3Sec");

        // Working entities start with P1's values; tertiary is unassigned (null)
        var e1 = new TestdataMultiVarEntity("e1", p1e1Prim, p1e1Sec, null);
        var e2 = new TestdataMultiVarEntity("e2", p1e2Prim, p1e2Sec, null);
        var e3 = new TestdataMultiVarEntity("e3", p1e3Prim, p1e3Sec, null);

        var workingSolution = new TestdataMultiVarSolution();
        workingSolution.setValueList(new ArrayList<>(List.of(
                p1e1Prim, p1e1Sec, p1e2Prim, p1e2Sec, p1e3Prim, p1e3Sec,
                p2e1Prim, p2e1Sec, p2e2Prim, p2e2Sec, p2e3Prim, p2e3Sec)));
        workingSolution.setOtherValueList(new ArrayList<>(List.of(p2e1Ter)));
        workingSolution.setMultiVarEntityList(new ArrayList<>(List.of(e1, e2, e3)));

        // First parent: e1=(p1e1Prim, p1e1Sec), e2=(p1e2Prim, p1e2Sec), e3=(p1e3Prim, p1e3Sec)
        var fp1e1 = new TestdataMultiVarEntity("e1", p1e1Prim, p1e1Sec, null);
        var fp1e2 = new TestdataMultiVarEntity("e2", p1e2Prim, p1e2Sec, null);
        var fp1e3 = new TestdataMultiVarEntity("e3", p1e3Prim, p1e3Sec, null);
        var firstParent = new TestdataMultiVarSolution();
        firstParent.setValueList(new ArrayList<>(List.of(p1e1Prim, p1e1Sec, p1e2Prim, p1e2Sec, p1e3Prim, p1e3Sec)));
        firstParent.setOtherValueList(new ArrayList<>());
        firstParent.setMultiVarEntityList(new ArrayList<>(List.of(fp1e1, fp1e2, fp1e3)));

        // Second parent: e1=(p2e1Prim, p2e1Sec), e2=(p2e2Prim, p2e2Sec), e3=(p2e3Prim, p2e3Sec)
        var fp2e1 = new TestdataMultiVarEntity("e1", p2e1Prim, p2e1Sec, p2e1Ter);
        var fp2e2 = new TestdataMultiVarEntity("e2", p2e2Prim, p2e2Sec, null);
        var fp2e3 = new TestdataMultiVarEntity("e3", p2e3Prim, p2e3Sec, null);
        var secondParent = new TestdataMultiVarSolution();
        secondParent.setValueList(new ArrayList<>(List.of(p2e1Prim, p2e1Sec, p2e2Prim, p2e2Sec, p2e3Prim, p2e3Sec)));
        secondParent.setOtherValueList(new ArrayList<>(List.of(p2e1Ter)));
        secondParent.setMultiVarEntityList(new ArrayList<>(List.of(fp2e1, fp2e2, fp2e3)));

        var scoreDirector = buildMultiVarScoreDirector();
        scoreDirector.setWorkingSolution(workingSolution);
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
        doReturn(mock(AbstractSolver.class)).when(solverScope).getSolver();

        var firstIndividual = (Individual<TestdataMultiVarSolution, SimpleScore>) mock(Individual.class);
        when(firstIndividual.size()).thenReturn(9);
        when(firstIndividual.getSolution()).thenReturn(firstParent);
        when(firstIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(firstIndividual.getChromosome()).thenReturn(buildMultiVarChromosome(firstParent));

        var secondIndividual = (Individual<TestdataMultiVarSolution, SimpleScore>) mock(Individual.class);
        when(secondIndividual.size()).thenReturn(9);
        when(secondIndividual.getSolution()).thenReturn(secondParent);
        when(secondIndividual.getScore()).thenReturn(InnerScore.fullyAssigned(SimpleScore.ZERO));
        when(secondIndividual.getChromosome()).thenReturn(buildMultiVarChromosome(secondParent));

        // generateIndexes(size=9, rate=0): nextInt(9) → 3, 4 → [3, 4]
        // fixIndex(p1, 3, true): target=e2 (first var of e2), chromosome[2].entity()=e1 ≠ e2 → return 3
        // fixIndex(p1, 4, false): target=e2 (second var of e2), scans forward: chromosome[5].entity()=e2
        //   → continue; chromosome[6].entity()=e3 ≠ e2 → return 6
        // Effective cut: P1 applies [3, 6) = all 3 variables of e2; P2 applies [0, 3) + [6, 9) = e1 and e3
        when(random.nextInt(9)).thenReturn(3, 4);

        var localSearchPhase = mock(Phase.class);
        var context =
                new CrossoverContext<TestdataMultiVarSolution, SimpleScore>(phaseScope, firstIndividual, secondIndividual);
        var result =
                new BasicOXCrossover<TestdataMultiVarSolution, SimpleScore>(localSearchPhase, null, 0, random).apply(context);
        var offspring = result.solution();
        var entities = offspring.getMultiVarEntityList();

        assertThat(entities.get(0).getPrimaryValue().getCode()).isEqualTo("p2e1Prim"); // e1 from P2
        assertThat(entities.get(0).getSecondaryValue().getCode()).isEqualTo("p2e1Sec");
        assertThat(entities.get(0).getTertiaryValueAllowedUnassigned().getCode()).isEqualTo("p2e1Ter");
        assertThat(entities.get(1).getPrimaryValue().getCode()).isEqualTo("p1e2Prim"); // e2 from P1
        assertThat(entities.get(1).getSecondaryValue().getCode()).isEqualTo("p1e2Sec");
        assertThat(entities.get(1).getTertiaryValueAllowedUnassigned()).isNull();
        assertThat(entities.get(2).getPrimaryValue().getCode()).isEqualTo("p2e3Prim"); // e3 from P2
        assertThat(entities.get(2).getSecondaryValue().getCode()).isEqualTo("p2e3Sec");
        assertThat(entities.get(2).getTertiaryValueAllowedUnassigned()).isNull();
    }
}
