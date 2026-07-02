package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.basic;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarConstraintProvider;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataOtherValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;

import org.junit.jupiter.api.Test;

class BasicSolutionDefaultStateManagerTest {

    private InnerScoreDirector<TestdataPinnedSolution, SimpleScore> buildPinnedScoreDirector() {
        var factory = new BavetConstraintStreamScoreDirectorFactory<TestdataPinnedSolution, SimpleScore>(
                TestdataPinnedSolution.buildSolutionDescriptor(),
                constraintFactory -> new Constraint[] { constraintFactory
                        .forEach(TestdataPinnedEntity.class).penalize(SimpleScore.ONE).asConstraint("Dummy constraint") },
                EnvironmentMode.FULL_ASSERT);
        return new BavetConstraintStreamScoreDirector.Builder<>(factory)
                .withLookUpEnabled(true)
                .build();
    }

    private InnerScoreDirector<TestdataMultiVarSolution, SimpleScore> buildMultiVarScoreDirector() {
        var factory = new BavetConstraintStreamScoreDirectorFactory<TestdataMultiVarSolution, SimpleScore>(
                TestdataMultiVarSolution.buildSolutionDescriptor(),
                new TestdataMultiVarConstraintProvider(),
                EnvironmentMode.FULL_ASSERT);
        return new BavetConstraintStreamScoreDirector.Builder<>(factory)
                .withLookUpEnabled(true)
                .build();
    }

    private InnerScoreDirector<TestdataSolution, SimpleScore> buildScoreDirector() {
        var factory = new BavetConstraintStreamScoreDirectorFactory<TestdataSolution, SimpleScore>(
                TestdataSolution.buildSolutionDescriptor(),
                constraintFactory -> new Constraint[] { constraintFactory
                        .forEach(TestdataEntity.class).penalize(SimpleScore.ONE).asConstraint("Dummy constraint") },
                EnvironmentMode.FULL_ASSERT);
        return new BavetConstraintStreamScoreDirector.Builder<>(factory)
                .withLookUpEnabled(true)
                .build();
    }

    @Test
    void saveStateWithAllVariablesAssigned() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);

        var solution = new TestdataSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of(e1, e2));

        var scoreDirector = mockScoreDirector(TestdataSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);

        var state = new BasicSolutionStateManager<TestdataSolution, SimpleScore>().saveSolutionState(scoreDirector, true);

        assertThat(state.stateList())
                .hasSize(2)
                .extracting(BasicValueState::value)
                .containsExactlyInAnyOrder(v1, v2);
    }

    @Test
    void saveStateWithNoVariablesAssigned() {
        var v1 = new TestdataValue("v1");
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");

        var solution = new TestdataSolution();
        solution.setValueList(List.of(v1));
        solution.setEntityList(List.of(e1, e2));

        var scoreDirector = mockScoreDirector(TestdataSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);

        var state = new BasicSolutionStateManager<TestdataSolution, SimpleScore>().saveSolutionState(scoreDirector, true);

        assertThat(state.stateList())
                .hasSize(2)
                .extracting(BasicValueState::value)
                .containsOnlyNulls();
    }

    @Test
    void saveStateWithoutAssignedValues() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);

        var solution = new TestdataSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of(e1, e2));

        var scoreDirector = mockScoreDirector(TestdataSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);

        var state = new BasicSolutionStateManager<TestdataSolution, SimpleScore>().saveSolutionState(scoreDirector, false);

        assertThat(state.stateList())
                .hasSize(2)
                .extracting(BasicValueState::value)
                .containsOnlyNulls();
    }

    @Test
    void restorePartialState() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);

        var solution = new TestdataSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(e1, e2));

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var manager = new BasicSolutionStateManager<TestdataSolution, SimpleScore>();

        var savedState = manager.saveSolutionState(scoreDirector, true);

        // Change e1's value after the snapshot
        var variableMetaModel = TestdataEntity.buildVariableDescriptorForValue().getVariableMetaModel();
        scoreDirector.executeMove(Moves.change(variableMetaModel, e1, v3));

        assertThat(e1.getValue()).isSameAs(v3);

        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(e1.getValue()).isSameAs(v1);
        assertThat(e2.getValue()).isSameAs(v2);
    }

    @Test
    void preserveState() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);

        var solution = new TestdataSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of(e1, e2));

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var manager = new BasicSolutionStateManager<TestdataSolution, SimpleScore>();

        var savedState = manager.saveSolutionState(scoreDirector, true);
        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(e1.getValue()).isSameAs(v1);
        assertThat(e2.getValue()).isSameAs(v2);
    }

    @Test
    void restoreStateWithRebase() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);

        var solution = new TestdataSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of(e1, e2));

        var scoreDirector = buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var manager = new BasicSolutionStateManager<TestdataSolution, SimpleScore>();

        var savedState = manager.saveSolutionState(scoreDirector, true);

        // Clone the working solution to force rebasing
        scoreDirector.setWorkingSolution(scoreDirector.cloneWorkingSolution());

        manager.restoreSolutionState(scoreDirector, savedState);

        var restoredEntities = scoreDirector.getWorkingSolution().getEntityList();
        assertThat(restoredEntities.get(0).getValue().getCode()).isEqualTo("v1");
        assertThat(restoredEntities.get(1).getValue().getCode()).isEqualTo("v2");
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveStateFromIndividualWithEmptyChromosome() {
        var solution = new TestdataSolution();
        solution.setValueList(List.of());
        solution.setEntityList(List.of());

        var individual = (Individual<TestdataSolution, SimpleScore>) mock(Individual.class);
        doReturn(solution).when(individual).getSolution();
        doReturn(new ChromosomeEntry[0]).when(individual).getChromosome();
        var score = (InnerScore<SimpleScore>) mock(InnerScore.class);
        doReturn(score).when(individual).getScore();

        var scoreDirector = buildScoreDirector();

        var state = new BasicSolutionStateManager<TestdataSolution, SimpleScore>().saveSolutionState(scoreDirector, individual);

        assertThat(state.getSolution()).isSameAs(solution);
        assertThat(state.stateList()).isEmpty();
        assertThat(state.getScore()).isSameAs(score);
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveStateFromIndividualWithAssignedValues() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);

        var solution = new TestdataSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of(e1, e2));

        var individual = (Individual<TestdataSolution, SimpleScore>) mock(Individual.class);
        doReturn(solution).when(individual).getSolution();
        // index=0 means the first (and only) basic variable descriptor for TestdataEntity
        doReturn(new ChromosomeEntry[] {
                new ChromosomeEntry(e1, v1, 0),
                new ChromosomeEntry(e2, v2, 0)
        }).when(individual).getChromosome();
        var score = (InnerScore<SimpleScore>) mock(InnerScore.class);
        doReturn(score).when(individual).getScore();

        var scoreDirector = buildScoreDirector();

        var state = new BasicSolutionStateManager<TestdataSolution, SimpleScore>().saveSolutionState(scoreDirector, individual);

        assertThat(state.getSolution()).isSameAs(solution);
        assertThat(state.getScore()).isSameAs(score);

        var entityValueList = state.stateList();
        assertThat(entityValueList).hasSize(2);

        assertThat(entityValueList.get(0).entity()).isSameAs(e1);
        assertThat(entityValueList.get(0).value()).isSameAs(v1);
        assertThat(entityValueList.get(0).index()).isZero();

        assertThat(entityValueList.get(1).entity()).isSameAs(e2);
        assertThat(entityValueList.get(1).value()).isSameAs(v2);
        assertThat(entityValueList.get(1).index()).isZero();
    }

    @Test
    void savePartialStatePreservesPinnedValues() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataPinnedEntity("e1", v1, true);
        var e2 = new TestdataPinnedEntity("e2", v2, false);

        var solution = new TestdataPinnedSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of(e1, e2));

        var scoreDirector = buildPinnedScoreDirector();
        scoreDirector.setWorkingSolution(solution);

        var state = new BasicSolutionStateManager<TestdataPinnedSolution, SimpleScore>()
                .saveSolutionState(scoreDirector, false);

        assertThat(state.stateList()).hasSize(2);
        // Pinned entity's value is saved even when saveAssigned=false
        assertThat(state.stateList()).filteredOn(s -> s.entity() == e1)
                .singleElement().extracting(BasicValueState::value).isSameAs(v1);
        // Unpinned entity's value is not saved when saveAssigned=false
        assertThat(state.stateList()).filteredOn(s -> s.entity() == e2)
                .singleElement().extracting(BasicValueState::value).isNull();
    }

    @Test
    void restorePartialStateWithPinnedEntities() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var e1 = new TestdataPinnedEntity("e1", v1, true);
        var e2 = new TestdataPinnedEntity("e2", v2, false);

        var solution = new TestdataPinnedSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(e1, e2));

        var scoreDirector = buildPinnedScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var manager = new BasicSolutionStateManager<TestdataPinnedSolution, SimpleScore>();

        var savedState = manager.saveSolutionState(scoreDirector, true);

        // Change the unpinned entity's value after the snapshot
        var variableMetaModel = TestdataPinnedEntity.buildEntityDescriptor()
                .getBasicVariableDescriptorList().get(0).getVariableMetaModel();
        scoreDirector.executeMove(Moves.change(variableMetaModel, e2, v3));
        assertThat(e2.getValue()).isSameAs(v3);

        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(e1.getValue()).isSameAs(v1);
        assertThat(e2.getValue()).isSameAs(v2);
    }

    @Test
    void savePartialStateWithMultipleBasicVariables() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var ov1 = new TestdataOtherValue("ov1");
        var e1 = new TestdataMultiVarEntity("e1", v1, v2, ov1);
        var e2 = new TestdataMultiVarEntity("e2", v2, v1, null);

        var solution = new TestdataMultiVarSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setOtherValueList(List.of(ov1));
        solution.setMultiVarEntityList(List.of(e1, e2));

        var scoreDirector = mockScoreDirector(TestdataMultiVarSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);

        var state =
                new BasicSolutionStateManager<TestdataMultiVarSolution, SimpleScore>().saveSolutionState(scoreDirector, true);

        // 2 entities × 3 basic variables each (primaryValue, secondaryValue, tertiaryValueAllowedUnassigned)
        assertThat(state.stateList()).hasSize(6);

        var e1States = state.stateList().stream().filter(s -> s.entity() == e1).toList();
        assertThat(e1States).hasSize(3);
        assertThat(e1States).extracting(BasicValueState::value).containsExactlyInAnyOrder(v1, v2, ov1);

        var e2States = state.stateList().stream().filter(s -> s.entity() == e2).toList();
        assertThat(e2States).hasSize(3);
        assertThat(e2States).extracting(BasicValueState::value).containsExactlyInAnyOrder(v2, v1, null);
    }

    @Test
    void restorePartialStateWithMultipleBasicVariables() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var ov1 = new TestdataOtherValue("ov1");
        var ov2 = new TestdataOtherValue("ov2");
        var ov3 = new TestdataOtherValue("ov3");
        var e1 = new TestdataMultiVarEntity("e1", v1, v2, ov1);
        var e2 = new TestdataMultiVarEntity("e2", v2, v1, ov2);

        var solution = new TestdataMultiVarSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setOtherValueList(List.of(ov1, ov2, ov3));
        solution.setMultiVarEntityList(List.of(e1, e2));

        var scoreDirector = buildMultiVarScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var manager = new BasicSolutionStateManager<TestdataMultiVarSolution, SimpleScore>();

        var savedState = manager.saveSolutionState(scoreDirector, true);

        // Change primaryValue of e1 and secondaryValue of e2 after the snapshot
        var primaryMeta = TestdataMultiVarEntity.buildVariableDescriptorForPrimaryValue().getVariableMetaModel();
        var secondaryMeta = TestdataMultiVarEntity.buildVariableDescriptorForSecondaryValue().getVariableMetaModel();
        var tertiaryMeta = TestdataMultiVarEntity.buildVariableDescriptorForTertiaryValue().getVariableMetaModel();
        scoreDirector.executeMove(Moves.change(primaryMeta, e1, v3));
        scoreDirector.executeMove(Moves.change(secondaryMeta, e2, v3));
        scoreDirector.executeMove(Moves.change(tertiaryMeta, e2, ov3));

        assertThat(e1.getPrimaryValue()).isSameAs(v3);
        assertThat(e2.getSecondaryValue()).isSameAs(v3);
        assertThat(e2.getTertiaryValueAllowedUnassigned()).isSameAs(ov3);

        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(e1.getPrimaryValue()).isSameAs(v1);
        assertThat(e1.getSecondaryValue()).isSameAs(v2);
        assertThat(e1.getTertiaryValueAllowedUnassigned()).isSameAs(ov1);
        assertThat(e2.getPrimaryValue()).isSameAs(v2);
        assertThat(e2.getSecondaryValue()).isSameAs(v1);
        assertThat(e2.getTertiaryValueAllowedUnassigned()).isSameAs(ov2);
    }
}
