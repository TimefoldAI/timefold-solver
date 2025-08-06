package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getAllowsUnassignedvaluesEntityRangeListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getEntityRangeListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getFilteringValueRangeSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getMimicRecordingIterableValueSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getPinnedEntityRangeListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockIterableValueSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelectorWithoutSize;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCodesOfNeverEndingMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;

import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
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

class ListSwapMoveSelectorTest {

    @Test
    void original() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var e1 = TestdataListEntity.createWithValues("A", v2, v1);
        var e2 = TestdataListEntity.createWithValues("B");
        var e3 = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2, e3));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                mockIterableValueSelector(listVariableDescriptor, v3, v1, v2),
                mockIterableValueSelector(listVariableDescriptor, v3, v1, v2),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        assertAllCodesOfMoveSelector(moveSelector,
                "No change", // ephemeral
                "v3 {C[0]} <-> v1 {A[1]}",
                "v3 {C[0]} <-> v2 {A[0]}",
                "v1 {A[1]} <-> v3 {C[0]}", // redundant
                "No change", // ephemeral
                "v1 {A[1]} <-> v2 {A[0]}",
                "v2 {A[0]} <-> v3 {C[0]}", // redundant
                "v2 {A[0]} <-> v1 {A[1]}", // redundant
                "No change" // ephemeral
        );
    }

    @Test
    void originalWithEntityValueRange() {
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
                getEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), false);

        var filteringValueRangeSelector = getFilteringValueRangeSelector(mimicRecordingValueSelector, false, true);

        var moveSelector = new ListSwapMoveSelector<>(mimicRecordingValueSelector, filteringValueRangeSelector, false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(solverScope, moveSelector);

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "1 {A[1]} <-> 2 {A[0]}",
                "2 {A[0]} <-> 1 {A[1]}",
                "2 {A[0]} <-> 3 {B[0]}",
                "3 {B[0]} <-> 2 {A[0]}");
    }

    @Test
    void originalWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("v1");
        var v2 = new TestdataPinnedWithIndexListValue("v2");
        var v3 = new TestdataPinnedWithIndexListValue("v3");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v2, v1);
        a.setPlanningPinToIndex(1); // Ignore v2
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B");
        b.setPinned(true); // Ignore entirely.
        var c = TestdataPinnedWithIndexListEntity.createWithValues("C", v3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var listVariableDescriptor = getPinnedListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                mockIterableValueSelector(listVariableDescriptor, v3, v1, v2),
                mockIterableValueSelector(listVariableDescriptor, v3, v1, v2),
                false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "No change", // ephemeral
                "v3 {C[0]} <-> v1 {A[1]}",
                "v1 {A[1]} <-> v3 {C[0]}", // redundant
                "No change" // ephemeral
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
        var c = new TestdataListPinnedEntityProvidingEntity("C", List.of(v1, v3, v4));
        c.setValueList(List.of(v3));
        var solution = new TestdataListPinnedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getPinnedEntityRangeListVariableDescriptor(scoreDirector).getValueRangeDescriptor(), false);

        var filteringValueRangeSelector = getFilteringValueRangeSelector(mimicRecordingValueSelector, false, true);

        var moveSelector = new ListSwapMoveSelector<>(mimicRecordingValueSelector, filteringValueRangeSelector, false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(solverScope, moveSelector);

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "1 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 1 {A[1]}");
    }

    @Test
    void originalAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("v3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("v4");
        var a = TestdataAllowsUnassignedValuesListEntity.createWithValues("A", v2, v1);
        var b = TestdataAllowsUnassignedValuesListEntity.createWithValues("B", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        // swap moves do not support uninitialized entities
        var listVariableDescriptor = getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                mockIterableValueSelector(listVariableDescriptor, v1, v2, v3, v4),
                mockIterableValueSelector(listVariableDescriptor, v4, v3, v2, v1),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // Tests each move from the product of the two value selectors.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "v1 {A[1]->null}+v4 {null->A[1]}",
                "v1 {A[1]} <-> v3 {B[0]}",
                "v1 {A[1]} <-> v2 {A[0]}",
                "No change",
                "v2 {A[0]->null}+v4 {null->A[0]}",
                "v2 {A[0]} <-> v3 {B[0]}",
                "No change",
                "v2 {A[0]} <-> v1 {A[1]}",
                "v3 {B[0]->null}+v4 {null->B[0]}",
                "No change",
                "v3 {B[0]} <-> v2 {A[0]}",
                "v3 {B[0]} <-> v1 {A[1]}",
                "No change",
                "v3 {B[0]->null}+v4 {null->B[0]}",
                "v2 {A[0]->null}+v4 {null->A[0]}",
                "v1 {A[1]->null}+v4 {null->A[1]}");
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

        var filteringValueRangeSelector = getFilteringValueRangeSelector(mimicRecordingValueSelector, false, true);

        var moveSelector = new ListSwapMoveSelector<>(mimicRecordingValueSelector, filteringValueRangeSelector, false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(solverScope, moveSelector);

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "1 {A[1]} <-> 2 {A[0]}",
                "2 {A[0]} <-> 1 {A[1]}");
    }

    @Test
    void random() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var e1 = TestdataListEntity.createWithValues("A", v1, v2);
        var e2 = TestdataListEntity.createWithValues("B");
        var e3 = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2, e3));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                // Value selectors are longer than the number of expected codes because they're expected
                // to be never ending, so they must not be exhausted after the last asserted code.
                mockIterableValueSelector(listVariableDescriptor, v2, v3, v2, v3, v2, v3, v1, v1, v1, v1),
                mockIterableValueSelector(listVariableDescriptor, v1, v2, v3, v1, v2, v3, v1, v2, v3, v1),
                true);

        solvingStarted(moveSelector, scoreDirector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]} <-> 1 {A[0]}",
                "3 {C[0]} <-> 2 {A[1]}",
                "2 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 1 {A[0]}",
                "No change",
                "No change",
                "No change",
                "1 {A[0]} <-> 2 {A[1]}",
                "1 {A[0]} <-> 3 {C[0]}");
    }

    @Test
    void randomWithEntityValueRange() {
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
                getEntityRangeListVariableDescriptor(scoreDirector), v2, v1, v3, v1, v3, v2, v1, v3, v1, v3);

        var filteringValueRangeSelector = getFilteringValueRangeSelector(mimicRecordingValueSelector, true, true);

        var moveSelector = new ListSwapMoveSelector<>(mimicRecordingValueSelector, filteringValueRangeSelector, true);

        var solverScope = solvingStarted(moveSelector, scoreDirector, new Random(0));
        phaseStarted(solverScope, moveSelector);

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[0]} <-> 3 {B[0]}",
                "1 {A[1]} <-> 2 {A[0]}",
                "3 {B[0]} <-> 2 {A[0]}",
                "1 {A[1]} <-> 2 {A[0]}",
                "3 {B[0]} <-> 2 {A[0]}");
    }

    @Test
    void randomWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v1, v2);
        a.setPlanningPinToIndex(1); // Ignore v1
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B");
        b.setPinned(true); // Ignore entirely.
        var c = TestdataPinnedWithIndexListEntity.createWithValues("C", v3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var listVariableDescriptor = getPinnedListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                // Value selectors are longer than the number of expected codes because they're expected
                // to be never ending, so they must not be exhausted after the last asserted code.
                mockIterableValueSelector(listVariableDescriptor, v1, v2, v3, v1, v2, v3, v1, v2, v3),
                mockIterableValueSelector(listVariableDescriptor, v1, v3, v2, v1, v3, v2, v1, v3, v2),
                true);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 2 {A[1]}",
                "2 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 2 {A[1]}",
                "2 {A[1]} <-> 3 {C[0]}");
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
        var c = new TestdataListPinnedEntityProvidingEntity("C", List.of(v1, v3, v4));
        c.setValueList(List.of(v3));
        var solution = new TestdataListPinnedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getPinnedEntityRangeListVariableDescriptor(scoreDirector), v3, v1, v4, v1);

        var filteringValueRangeSelector = getFilteringValueRangeSelector(mimicRecordingValueSelector, true, true);

        var moveSelector = new ListSwapMoveSelector<>(mimicRecordingValueSelector, filteringValueRangeSelector, true);

        var solverScope = solvingStarted(moveSelector, scoreDirector, new Random(0));
        phaseStarted(solverScope, moveSelector);

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "3 {C[0]} <-> 1 {A[1]}",
                "1 {A[1]} <-> 3 {C[0]}");
    }

    @Test
    void randomAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("4");
        var e1 = TestdataAllowsUnassignedValuesListEntity.createWithValues("A", v2, v1);
        var e2 = TestdataAllowsUnassignedValuesListEntity.createWithValues("B", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        // swap moves do not support uninitialized entities
        var listVariableDescriptor = getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                // Value selectors are longer than the number of expected codes because they're expected
                // to be never ending, so they must not be exhausted after the last asserted code.
                mockIterableValueSelector(listVariableDescriptor, v2, v3, v4, v2, v3, v4, v2, v3, v4, v1, v1, v1, v1),
                mockIterableValueSelector(listVariableDescriptor, v1, v2, v3, v4, v1, v2, v3, v4, v1, v2, v3, v1, v4),
                true);

        solvingStarted(moveSelector, scoreDirector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[0]} <-> 1 {A[1]}",
                "3 {B[0]} <-> 2 {A[0]}",
                "3 {B[0]->null}+4 {null->B[0]}",
                "2 {A[0]->null}+4 {null->A[0]}",
                "3 {B[0]} <-> 1 {A[1]}",
                "2 {A[0]->null}+4 {null->A[0]}",
                "2 {A[0]} <-> 3 {B[0]}",
                "3 {B[0]->null}+4 {null->B[0]}",
                "1 {A[1]->null}+4 {null->A[1]}");
    }

    @Test
    void randomAllowsUnassignedValuesWithEntityValueRange() {
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var v4 = new TestdataValue("4");
        var a = new TestdataListUnassignedEntityProvidingEntity("A", List.of(v1, v2, v3, v4));
        a.setValueList(List.of(v2, v1));
        var b = new TestdataListUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        b.setValueList(List.of(v3));
        var solution = new TestdataListUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b));

        var scoreDirector = mockScoreDirector(TestdataListUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var mimicRecordingValueSelector = getMimicRecordingIterableValueSelector(
                getAllowsUnassignedvaluesEntityRangeListVariableDescriptor(scoreDirector), v3, v1, v2, v4, v1);

        var filteringValueRangeSelector = getFilteringValueRangeSelector(mimicRecordingValueSelector, true, true);

        var moveSelector = new ListSwapMoveSelector<>(mimicRecordingValueSelector, filteringValueRangeSelector, true);

        var solverScope = solvingStarted(moveSelector, scoreDirector, new Random(0));
        phaseStarted(solverScope, moveSelector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "3 {B[0]} <-> 2 {A[0]}",
                "1 {A[1]->null}+4 {null->A[1]}",
                "2 {A[0]->null}+4 {null->A[0]}",
                "1 {A[1]->null}+4 {null->A[1]}");
    }
}
