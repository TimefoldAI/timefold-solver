package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.list;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListValue;

import org.junit.jupiter.api.Test;

class ListSolutionStateManagerTest {

    private <Solution_> InnerScoreDirector<Solution_, SimpleScore> buildScoreDirector(boolean pinned) {
        InnerScoreDirector<Solution_, SimpleScore> scoreDirector;
        if (pinned) {
            var factory = new BavetConstraintStreamScoreDirectorFactory<TestdataPinnedListSolution, SimpleScore>(
                    TestdataPinnedListSolution.buildSolutionDescriptor(),
                    constraintFactory -> new Constraint[] { constraintFactory
                            .forEach(TestdataPinnedListEntity.class).penalize(SimpleScore.ONE)
                            .asConstraint("Dummy constraint") },
                    EnvironmentMode.FULL_ASSERT);
            scoreDirector =
                    (InnerScoreDirector<Solution_, SimpleScore>) new BavetConstraintStreamScoreDirector.Builder<>(
                            factory)
                            .withLookUpEnabled(true)
                            .build();
        } else {
            var factory = new BavetConstraintStreamScoreDirectorFactory<TestdataListSolution, SimpleScore>(
                    TestdataListSolution.buildSolutionDescriptor(), constraintFactory -> new Constraint[] { constraintFactory
                            .forEach(TestdataListEntity.class).penalize(SimpleScore.ONE).asConstraint("Dummy constraint") },
                    EnvironmentMode.FULL_ASSERT);
            scoreDirector =
                    (InnerScoreDirector<Solution_, SimpleScore>) new BavetConstraintStreamScoreDirector.Builder<>(
                            factory)
                            .withLookUpEnabled(true)
                            .build();
        }
        return scoreDirector;
    }

    @Test
    void saveStateWithNoValuesAssigned() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(a, b));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);

        var state = new ListSolutionStateManager<TestdataListSolution, SimpleScore>()
                .saveSolutionState(scoreDirector, true);

        assertThat(state.assignedValueList()).isEmpty();
    }

    @Test
    void saveStateWithAllValuesAssigned() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(a, b));
        a.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);

        var state = new ListSolutionStateManager<TestdataListSolution, SimpleScore>()
                .saveSolutionState(scoreDirector, true);

        assertThat(state.assignedValueList())
                .hasSize(3)
                .extracting(lv -> ((TestdataListValue) lv.value()).getCode())
                .containsExactlyInAnyOrder("v1", "v2", "v3");
    }

    @Test
    void saveStateWithoutAssignedValues() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(a, b));
        a.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);

        var state = new ListSolutionStateManager<TestdataListSolution, SimpleScore>()
                .saveSolutionState(scoreDirector, false);

        assertThat(state.assignedValueList()).isEmpty();
    }

    @Test
    void saveStateWithPartiallyAssigned() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(a, b));
        // v3 left unassigned
        a.setValueList(List.of(v1, v2));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);

        var state = new ListSolutionStateManager<TestdataListSolution, SimpleScore>()
                .saveSolutionState(scoreDirector, true);

        assertThat(state.assignedValueList())
                .hasSize(2)
                .extracting(lv -> ((TestdataListValue) lv.value()).getCode())
                .containsExactlyInAnyOrder("v1", "v2");
    }

    @Test
    void restoreEmptyState() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(a, b));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor(), true);
        scoreDirector.setWorkingSolution(solution);
        var manager = new ListSolutionStateManager<TestdataListSolution, SimpleScore>();

        // Save state while nothing is assigned
        var emptyState = manager.saveSolutionState(scoreDirector, true);

        // Restore to empty state
        a.setValueList(new ArrayList<>(List.of(v1, v2, v3)));
        scoreDirector.setWorkingSolution(solution);
        manager.restoreSolutionState(scoreDirector, emptyState);

        assertThat(a.getValueList()).isEmpty();
        assertThat(b.getValueList()).isEmpty();
    }

    @Test
    void restorePartialState() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(a, b));
        a.setValueList(new ArrayList<>(List.of(v1, v2)));

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector = buildScoreDirector(false);
        scoreDirector.setWorkingSolution(solution);
        var manager = new ListSolutionStateManager<TestdataListSolution, SimpleScore>();

        var savedState = manager.saveSolutionState(scoreDirector, true);

        // Assign v3 after the snapshot
        scoreDirector.executeMove(Moves
                .assign(scoreDirector.getSolutionDescriptor().getListVariableDescriptor().getVariableMetaModel(), v3, b, 0));

        // Restore: v3 should be unassigned, v1 and v2 stay at original positions
        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(a.getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2");
        assertThat(b.getValueList()).isEmpty();
    }

    @Test
    void restoreStateFromMultipleEntities() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var v4 = new TestdataListValue("v4");
        var v5 = new TestdataListValue("v5");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3, v4, v5));
        solution.setEntityList(List.of(a, b));
        a.setValueList(new ArrayList<>(List.of(v1, v2)));
        b.setValueList(new ArrayList<>(List.of(v3)));

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector = buildScoreDirector(false);
        scoreDirector.setWorkingSolution(solution);
        var manager = new ListSolutionStateManager<TestdataListSolution, SimpleScore>();
        var savedState = manager.saveSolutionState(scoreDirector, true);

        // Add more values after snapshot: a=[v1,v2,v4], b=[v3,v5]
        var variableMetaModel = scoreDirector.getSolutionDescriptor().getListVariableDescriptor().getVariableMetaModel();
        scoreDirector.executeMove(Moves.assign(variableMetaModel, v4, a, 2));
        scoreDirector.executeMove(Moves.assign(variableMetaModel, v5, b, 1));

        // Restore: v4 and v5 should be unassigned, v1 and v2 back to a[0], v3 back to b[0]
        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(a.getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2");
        assertThat(b.getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v3");
    }

    @Test
    void preserveState() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of(a, b));
        a.setValueList(new ArrayList<>(List.of(v1, v2)));
        SolutionManager.updateShadowVariables(solution);

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector = buildScoreDirector(false);
        scoreDirector.setWorkingSolution(solution);
        var manager = new ListSolutionStateManager<TestdataListSolution, SimpleScore>();

        // Assign v1 and v2, save state, then immediately restore
        var savedState = manager.saveSolutionState(scoreDirector, true);

        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(a.getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2");
        assertThat(b.getValueList()).isEmpty();
    }

    @Test
    void restoreStateWithRebase() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var v4 = new TestdataListValue("v4");
        var v5 = new TestdataListValue("v5");

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3, v4, v5));
        solution.setEntityList(List.of(a, b));
        a.setValueList(new ArrayList<>(List.of(v1, v2)));
        b.setValueList(new ArrayList<>(List.of(v3)));

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector = buildScoreDirector(false);
        scoreDirector.setWorkingSolution(solution);
        var manager = new ListSolutionStateManager<TestdataListSolution, SimpleScore>();
        var savedState = manager.saveSolutionState(scoreDirector, true);

        // Add more values after snapshot: a=[v1,v2,v4], b=[v3,v5]
        a.getValueList().add(v4);
        b.getValueList().add(v5);
        // Update the working solution to force the rebasing
        scoreDirector.setWorkingSolution(scoreDirector.cloneWorkingSolution());

        // Restore: v4 and v5 should be unassigned, v1 and v2 back to a[0], v3 back to b[0]
        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(scoreDirector.getWorkingSolution().getEntityList().get(0).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v1", "v2");
        assertThat(scoreDirector.getWorkingSolution().getEntityList().get(1).getValueList())
                .extracting(TestdataListValue::getCode)
                .containsExactly("v3");
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveStateFromIndividualWithEmptyChromosome() {
        var solution = new TestdataListSolution();
        solution.setValueList(List.of());
        solution.setEntityList(List.of());

        var individual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        doReturn(solution).when(individual).getSolution();
        doReturn(new ChromosomeEntry[0]).when(individual).getChromosome();
        var score = (InnerScore<SimpleScore>) mock(InnerScore.class);
        doReturn(score).when(individual).getScore();

        var state = new ListSolutionStateManager<TestdataListSolution, SimpleScore>()
                .saveSolutionState(individual);

        assertThat(state.getSolution()).isSameAs(solution);
        assertThat(state.assignedValueList()).isEmpty();
        assertThat(state.getScore()).isSameAs(score);
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveStateFromIndividualWithAssignedValues() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2, v3));
        solution.setEntityList(List.of(a, b));

        var individual = (Individual<TestdataListSolution, SimpleScore>) mock(Individual.class);
        doReturn(solution).when(individual).getSolution();
        doReturn(new ChromosomeEntry[] {
                new ChromosomeEntry(v1, a, 0),
                new ChromosomeEntry(v2, a, 1),
                new ChromosomeEntry(v3, b, 0)
        }).when(individual).getChromosome();
        var score = (InnerScore<SimpleScore>) mock(InnerScore.class);
        doReturn(score).when(individual).getScore();

        var state = new ListSolutionStateManager<TestdataListSolution, SimpleScore>()
                .saveSolutionState(individual);

        assertThat(state.getSolution()).isSameAs(solution);
        assertThat(state.getScore()).isSameAs(score);

        var assignedValues = state.assignedValueList();
        assertThat(assignedValues).hasSize(3);

        assertThat(assignedValues.get(0).value()).isSameAs(v1);
        assertThat((Object) assignedValues.get(0).positionInList().entity()).isSameAs(a);
        assertThat(assignedValues.get(0).positionInList().index()).isZero();

        assertThat(assignedValues.get(1).value()).isSameAs(v2);
        assertThat((Object) assignedValues.get(1).positionInList().entity()).isSameAs(a);
        assertThat(assignedValues.get(1).positionInList().index()).isEqualTo(1);

        assertThat(assignedValues.get(2).value()).isSameAs(v3);
        assertThat((Object) assignedValues.get(2).positionInList().entity()).isSameAs(b);
        assertThat(assignedValues.get(2).positionInList().index()).isZero();
    }

    @Test
    void restorePartialStateWithOnlyPinned() {
        var v1 = new TestdataPinnedListValue("v1");
        var v2 = new TestdataPinnedListValue("v2");
        var v3 = new TestdataPinnedListValue("v3");
        var v4 = new TestdataPinnedListValue("v4");

        // Entity a is pinned — its values (v1, v2) will be captured by onlyPinned=true.
        var a = new TestdataPinnedListEntity("a", v1, v2);
        a.setPinned(true);
        // Entity b is not pinned — v3 is assigned but will not be captured by onlyPinned=true.
        var b = new TestdataPinnedListEntity("b", v3);

        var solution = new TestdataPinnedListSolution();
        solution.setValueList(new ArrayList<>(List.of(v1, v2, v3, v4)));
        solution.setEntityList(List.of(a, b));

        InnerScoreDirector<TestdataPinnedListSolution, SimpleScore> scoreDirector = buildScoreDirector(true);
        scoreDirector.setWorkingSolution(solution);
        var manager = new ListSolutionStateManager<TestdataPinnedListSolution, SimpleScore>();

        // Save no values
        var savedState = manager.saveSolutionState(scoreDirector, false);

        // Assign v4 to entity b after the snapshot — b now holds [v3, v4].
        var variableMetaModel = scoreDirector.getSolutionDescriptor().getListVariableDescriptor().getVariableMetaModel();
        scoreDirector.executeMove(Moves.assign(variableMetaModel, v4, b, 1));

        // Restore: all non-pinned values (v3, v4) are unassigned; pinned v1, v2 remain in entity a.
        manager.restoreSolutionState(scoreDirector, savedState);

        assertThat(a.getValueList())
                .extracting(TestdataPinnedListValue::getCode)
                .containsExactly("v1", "v2");
        assertThat(b.getValueList()).isEmpty();
    }
}
