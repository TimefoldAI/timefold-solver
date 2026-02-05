package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getAllowsUnassignedvaluesEntityRangeListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getEntityRangeListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getEntityValueRangeDestinationSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getIterableFromEntityPropertyValueSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getMimicRecordingIterableValueSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getPinnedEntityRangeListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockAllowsUnassignedValuesNeverEndingDestinationSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockDestinationSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockIterableValueSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockNeverEndingDestinationSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockPinnedEntityRangeNeverEndingDestinationSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockPinnedNeverEndingDestinationSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelectorWithoutSize;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCodesOfNeverEndingMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;

import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListUtils;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class ListChangeMoveSelectorTest {

    @Test
    void original() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var a = new TestdataListEntity("A", v2, v1);
        var b = new TestdataListEntity("B");
        var c = new TestdataListEntity("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockIterableValueSelector(getListVariableDescriptor(scoreDirector), v3, v1, v2),
                mockDestinationSelector(
                        ElementPosition.of(a, 0),
                        ElementPosition.of(b, 0),
                        ElementPosition.of(c, 0),
                        ElementPosition.of(c, 1),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1)),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        assertAllCodesOfMoveSelector(moveSelector,
                // Moving 3 from C[0]
                "3 {C[0]->A[0]}",
                "3 {C[0]->B[0]}",
                "3 {C[0]->C[0]}", // ephemeral
                "3 {C[0]->C[1]}", // ephemeral
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                // Moving 1 from A[1]
                "1 {A[1]->A[0]}",
                "1 {A[1]->B[0]}",
                "1 {A[1]->C[0]}",
                "1 {A[1]->C[1]}",
                "1 {A[1]->A[2]}", // ephemeral
                "1 {A[1]->A[1]}", // ephemeral
                // Moving 2 from A[0]
                "2 {A[0]->A[0]}", // ephemeral
                "2 {A[0]->B[0]}",
                "2 {A[0]->C[0]}",
                "2 {A[0]->C[1]}",
                "2 {A[0]->A[2]}", // ephemeral
                "2 {A[0]->A[1]}");
    }

    @Test
    void originalWithEntityValueRange() {
        var v1 = new TestdataListEntityProvidingValue("1");
        var v2 = new TestdataListEntityProvidingValue("2");
        var v3 = new TestdataListEntityProvidingValue("3");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2), List.of(v2, v1));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), false);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var destinationSelector = getEntityValueRangeDestinationSelector(mimicRecordingValueSelector, solutionDescriptor,
                entityDescriptor, false);
        var moveSelector = new ListChangeMoveSelector<>(mimicRecordingValueSelector, destinationSelector, false);

        var solverScope = solvingStarted(moveSelector, scoreDirector, mimicRecordingValueSelector, destinationSelector);
        phaseStarted(solverScope, mimicRecordingValueSelector, destinationSelector);

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "1 {A[1]->A[0]}",
                "1 {A[1]->A[1]}",
                "2 {A[0]->A[0]}",
                "2 {A[0]->B[0]}",
                "2 {A[0]->A[2]}",
                "2 {A[0]->B[1]}",
                "3 {B[0]->B[0]}");
    }

    @Test
    void originalWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var v4 = new TestdataPinnedWithIndexListValue("4");
        var a = new TestdataPinnedWithIndexListEntity("A", v2, v1);
        a.setPlanningPinToIndex(1); // Ignore v2.
        var b = new TestdataPinnedWithIndexListEntity("B", v4);
        b.setPinned(true); // Ignore entirely.
        var c = new TestdataPinnedWithIndexListEntity("C", v3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockIterableValueSelector(getPinnedListVariableDescriptor(scoreDirector), v4, v3, v1, v2),
                mockDestinationSelector(
                        ElementPosition.of(c, 0),
                        ElementPosition.of(c, 1),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1)),
                false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                // Moving 3 from C[0]
                "3 {C[0]->C[0]}", // ephemeral
                "3 {C[0]->C[1]}", // ephemeral
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                // Moving 1 from A[1]
                "1 {A[1]->C[0]}",
                "1 {A[1]->C[1]}",
                "1 {A[1]->A[2]}", // ephemeral
                "1 {A[1]->A[1]}" // ephemeral
        );
    }

    @Test
    void originalWithPinningAndEntityValueRange() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var a = new TestdataListPinnedEntityProvidingEntity("A", List.of(v1, v2, v3));
        a.setValueList(List.of(v2, v1));
        a.setPlanningPinToIndex(1); // Ignore v2.
        var b = new TestdataListPinnedEntityProvidingEntity("B", List.of(v2, v4));
        b.setPinned(true); // Ignore entirely.
        b.setValueList(List.of(v4));
        var c = new TestdataListPinnedEntityProvidingEntity("C", List.of(v3, v4));
        c.setValueList(List.of(v3));
        var solution = new TestdataListPinnedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getPinnedEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), false);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListPinnedEntityProvidingEntity.class);
        var destinationSelector = getEntityValueRangeDestinationSelector(mimicRecordingValueSelector, solutionDescriptor,
                entityDescriptor, false);
        var moveSelector = new ListChangeMoveSelector<>(mimicRecordingValueSelector, destinationSelector, false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(solverScope, moveSelector);

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "1 {A[1]->A[1]}",
                "3 {C[0]->A[1]}",
                "3 {C[0]->C[0]}",
                "3 {C[0]->A[2]}");
    }

    @Test
    void originalAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("4");
        var a = new TestdataAllowsUnassignedValuesListEntity("A", v2, v1);
        var b = new TestdataAllowsUnassignedValuesListEntity("B");
        var c = new TestdataAllowsUnassignedValuesListEntity("C", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockIterableValueSelector(getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector), v3, v1, v4,
                        v2),
                mockDestinationSelector(
                        ElementPosition.of(a, 0),
                        ElementPosition.of(b, 0),
                        ElementPosition.of(c, 0),
                        ElementPosition.of(c, 1),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1),
                        ElementPosition.unassigned()),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // First try all destinations for v3 (which is originally at C[0]),
        // then v1 (originally at A[1]),
        // then v4 (unassigned),
        // then v2 (originally at A[0]).
        assertAllCodesOfMoveSelector(moveSelector,
                "3 {C[0]->A[0]}",
                "3 {C[0]->B[0]}",
                "3 {C[0]->C[0]}", // ephemeral
                "3 {C[0]->C[1]}",
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                "3 {C[0]->null}",
                "1 {A[1]->A[0]}",
                "1 {A[1]->B[0]}",
                "1 {A[1]->C[0]}",
                "1 {A[1]->C[1]}",
                "1 {A[1]->A[2]}",
                "1 {A[1]->A[1]}", // ephemeral
                "1 {A[1]->null}",
                "4 {null->A[0]}",
                "4 {null->B[0]}",
                "4 {null->C[0]}",
                "4 {null->C[1]}",
                "4 {null->A[2]}",
                "4 {null->A[1]}",
                "No change",
                "2 {A[0]->A[0]}", // ephemeral
                "2 {A[0]->B[0]}",
                "2 {A[0]->C[0]}",
                "2 {A[0]->C[1]}",
                "2 {A[0]->A[2]}",
                "2 {A[0]->A[1]}",
                "2 {A[0]->null}");
    }

    @Test
    void originalAllowsUnassignedValuesWithEntityValueRange() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var a = new TestdataListUnassignedEntityProvidingEntity("A", List.of(v1, v2));
        a.setValueList(List.of(v2, v1));
        var b = new TestdataListUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        b.setValueList(List.of(v3));
        var solution = new TestdataListUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b));

        var scoreDirector = mockScoreDirector(TestdataListUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getAllowsUnassignedvaluesEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), false);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListUnassignedEntityProvidingEntity.class);
        var destinationSelector = getEntityValueRangeDestinationSelector(mimicRecordingValueSelector, solutionDescriptor,
                entityDescriptor, false);
        var moveSelector = new ListChangeMoveSelector<>(mimicRecordingValueSelector, destinationSelector, false);

        var solverScope = solvingStarted(moveSelector, scoreDirector, mimicRecordingValueSelector, destinationSelector);
        phaseStarted(solverScope, mimicRecordingValueSelector, destinationSelector);
        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "1 {A[1]->A[0]}",
                "1 {A[1]->A[1]}",
                "1 {A[1]->null}",
                "2 {A[0]->A[0]}",
                "2 {A[0]->B[0]}",
                "2 {A[0]->A[2]}",
                "2 {A[0]->B[1]}",
                "2 {A[0]->null}",
                "3 {B[0]->B[0]}",
                "3 {B[0]->null}");
    }

    @Test
    void random() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var a = new TestdataListEntity("A", v1, v2);
        var b = new TestdataListEntity("B");
        var c = new TestdataListEntity("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                TestdataListUtils.mockNeverEndingIterableValueSelector(getListVariableDescriptor(scoreDirector), v2, v1, v3, v3,
                        v3),
                mockNeverEndingDestinationSelector(
                        ElementPosition.of(b, 0),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 0),
                        ElementPosition.of(a, 1),
                        ElementPosition.of(a, 2)),
                true);

        solvingStarted(moveSelector, scoreDirector);

        // Initial state:
        // - A [1, 2]
        // - B []
        // - C [3]

        // The moved values (2, 1, 3, 3, 3) are supplied by the source value selector and their source positions
        // are deduced using inverse relation and index supplies. The destinations (B[0], A[2], ...) are supplied
        // by the destination selector.
        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]->B[0]}",
                "1 {A[0]->A[2]}",
                "3 {C[0]->A[0]}",
                "3 {C[0]->A[1]}",
                "3 {C[0]->A[2]}");
    }

    @Test
    void randomWithEntityValueRange() {
        var v1 = new TestdataListEntityProvidingValue("1");
        var v2 = new TestdataListEntityProvidingValue("2");
        var v3 = new TestdataListEntityProvidingValue("3");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2), List.of(v2, v1));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), true);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var destinationSelector = getEntityValueRangeDestinationSelector(mimicRecordingValueSelector, solutionDescriptor,
                entityDescriptor, true);
        var moveSelector = new ListChangeMoveSelector<>(mimicRecordingValueSelector, destinationSelector, true);

        var solverScope = solvingStarted(moveSelector, scoreDirector, new Random(0));
        phaseStarted(solverScope, moveSelector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "1 {A[1]->A[1]}",
                "3 {B[0]->B[0]}",
                "3 {B[0]->B[0]}",
                "3 {B[0]->B[1]}",
                "3 {B[0]->B[1]}");
    }

    @Test
    void randomWithEntityValueRangeAndFiltering() {
        var v1 = new TestdataListEntityProvidingValue("1");
        var v2 = new TestdataListEntityProvidingValue("2");
        var v3 = new TestdataListEntityProvidingValue("3");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2, v3), List.of(v2, v1));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), true);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var destinationSelector = getEntityValueRangeDestinationSelector(mimicRecordingValueSelector, solutionDescriptor,
                entityDescriptor, IgnoreBValueSelectionFilter.class, true);
        var moveSelector = new ListChangeMoveSelector<>(mimicRecordingValueSelector, destinationSelector, true);

        var solverScope = solvingStarted(moveSelector, scoreDirector, new Random(0));
        phaseStarted(solverScope, moveSelector);

        // IgnoreBValueSelectionFilter is applied to the value selector used by the destination selector,
        // and that causes the B destination to become an invalid destination
        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "1 {A[1]->A[1]}",
                "3 {B[0]->A[0]}",
                "3 {B[0]->B[0]}",
                "3 {B[0]->A[2]}",
                "3 {B[0]->A[1]}");
    }

    @Test
    void randomWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var v4 = new TestdataPinnedWithIndexListValue("4");
        var a = new TestdataPinnedWithIndexListEntity("A", v1, v2);
        a.setPlanningPinToIndex(1); // Ignore v1.
        var b = new TestdataPinnedWithIndexListEntity("B", v4);
        b.setPinned(true); // Ignore entirely.
        var c = new TestdataPinnedWithIndexListEntity("C", v3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                TestdataListUtils.mockNeverEndingIterableValueSelector(getPinnedListVariableDescriptor(scoreDirector), v2, v1,
                        v4, v3,
                        v3),
                mockPinnedNeverEndingDestinationSelector(
                        ElementPosition.of(c, 0),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1),
                        ElementPosition.of(c, 0)),
                true);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        // Initial state:
        // - A [1, 2]
        // - B []
        // - C [3]

        // The moved values (2, 3, 2, 2) are supplied by the source value selector,
        // and their source positions are deduced using inverse relation and index supplies.
        // The destinations (A[1], A[2], ...) are supplied by the destination selector.
        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]->C[0]}",
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                "2 {A[1]->C[0]}");
    }

    @Test
    void randomWithPinningAndEntityValueRange() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var a = new TestdataListPinnedEntityProvidingEntity("A", List.of(v1, v2, v3));
        a.setValueList(List.of(v2, v1));
        a.setPlanningPinToIndex(1); // Ignore v2.
        var b = new TestdataListPinnedEntityProvidingEntity("B", List.of(v2, v4));
        b.setPinned(true); // Ignore entirely.
        b.setValueList(List.of(v4));
        var c = new TestdataListPinnedEntityProvidingEntity("C", List.of(v3, v4));
        c.setValueList(List.of(v3));
        var solution = new TestdataListPinnedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var iterablePropertySelector = getIterableFromEntityPropertyValueSelector(
                getPinnedEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), true);

        var moveSelector = new ListChangeMoveSelector<>(
                iterablePropertySelector,
                mockPinnedEntityRangeNeverEndingDestinationSelector(
                        ElementPosition.of(c, 0),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1),
                        ElementPosition.of(c, 0)),
                true);

        var solverScope = solvingStarted(moveSelector, scoreDirector, new Random(0), iterablePropertySelector);
        phaseStarted(moveSelector, solverScope);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "3 {C[0]->C[0]}",
                "1 {A[1]->A[2]}");
    }

    @Test
    void randomAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("4");
        var a = new TestdataAllowsUnassignedValuesListEntity("A", v1, v2);
        var b = new TestdataAllowsUnassignedValuesListEntity("B");
        var c = new TestdataAllowsUnassignedValuesListEntity("C", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                TestdataListUtils.mockNeverEndingIterableValueSelector(
                        getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                        v2, v1, v4, v3,
                        v3, v3),
                mockAllowsUnassignedValuesNeverEndingDestinationSelector(
                        ElementPosition.of(b, 0),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 0),
                        ElementPosition.of(a, 1),
                        ElementPosition.of(a, 2),
                        ElementPosition.unassigned()),
                true);

        solvingStarted(moveSelector, scoreDirector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]->B[0]}",
                "1 {A[0]->A[2]}",
                "4 {null->A[0]}",
                "3 {C[0]->A[1]}",
                "3 {C[0]->A[2]}");
    }

    @Test
    void randomAllowsUnassignedValuesWithEntityValueRange() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var a = new TestdataListUnassignedEntityProvidingEntity("A", List.of(v1, v2));
        a.setValueList(List.of(v2, v1));
        var b = new TestdataListUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        b.setValueList(List.of(v3));
        var solution = new TestdataListUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b));

        var scoreDirector = mockScoreDirector(TestdataListUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getAllowsUnassignedvaluesEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), true);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListUnassignedEntityProvidingEntity.class);
        var destinationSelector = getEntityValueRangeDestinationSelector(mimicRecordingValueSelector, solutionDescriptor,
                entityDescriptor, true);
        var moveSelector = new ListChangeMoveSelector<>(mimicRecordingValueSelector, destinationSelector, true);

        var solverScope =
                solvingStarted(moveSelector, scoreDirector, new Random(0));
        phaseStarted(solverScope, moveSelector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "1 {A[1]->A[1]}",
                "3 {B[0]->B[1]}",
                "3 {B[0]->B[1]}",
                "3 {B[0]->B[1]}",
                "3 {B[0]->B[1]}");
    }

    public static class IgnoreBValueSelectionFilter
            implements SelectionFilter<TestdataListEntityProvidingSolution, TestdataListEntityProvidingValue> {

        public IgnoreBValueSelectionFilter() {
            // Required for solver initialization
        }

        @Override
        public boolean accept(ScoreDirector<TestdataListEntityProvidingSolution> scoreDirector,
                TestdataListEntityProvidingValue selection) {
            return !selection.getEntity().getCode().equals("B");
        }
    }
}
