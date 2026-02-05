package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.mockReplayingValueSelector;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.stepStarted;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getEntityRangeListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getPinnedAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockEntitySelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockIterableFromEntityPropertyValueSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockIterableValueSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfIterableSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCodesOfNeverEndingIterableSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCodesOfNeverEndingIterableSelectorWithoutSize;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCodesOfNeverEndingIterator;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertEmptyNeverEndingIterableSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.FromSolutionEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.FilteringEntityByValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueRangeSelector;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
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
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class ElementDestinationSelectorTest {

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

        var entitySelector = mockEntitySelector(a, b, c);
        var valueSelector = mockIterableValueSelector(getListVariableDescriptor(scoreDirector), v3, v1, v2);
        var destinationSize = entitySelector.getSize() + valueSelector.getSize();

        var selector = new ElementDestinationSelector<>(entitySelector, valueSelector, false);

        solvingStarted(selector, scoreDirector);

        // Entity order: [A, B, C]
        // Value order: [3, 1, 2]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        assertAllCodesOfIterableSelector(selector, destinationSize,
                "A[0]",
                "B[0]",
                "C[0]",
                "C[1]",
                "A[2]",
                "A[1]");
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

        var entitySelector = mockEntitySelector(a, b, c, c);
        var valueSelector =
                TestdataListUtils.mockNeverEndingIterableValueSelector(getListVariableDescriptor(scoreDirector), v3, v2, v1);
        var destinationSize = entitySelector.getSize() + valueSelector.getSize();

        var selector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);

        // <4 => entity selector; >=4 => value selector
        var random = new TestRandom(
                0, // => A[0]
                4, // => v3 => C[1]
                1, // => B[0]
                5, // => v2 => A[2]
                6, // => v1 => A[1]
                2, // => C[0]
                -1); // (not tested)

        solvingStarted(selector, scoreDirector, random);

        // Initial state:
        // - A [1, 2]
        // - B []
        // - C [3]

        // The destinations (A[0], C[1], ...) depend on the random number, which decides whether entitySelector or valueSelector
        // will be used; and then on the order of entities and values in the mocked selectors.
        assertCodesOfNeverEndingIterableSelector(selector, destinationSize,
                "A[0]",
                "C[1]",
                "B[0]",
                "A[2]",
                "A[1]",
                "C[0]");

        random.assertIntBoundJustRequested((int) destinationSize);
    }

    @Test
    void originalWithEntityValueRange() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        // V1 is only reachable by A
        var valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v1);
        var selector = new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, false, false);
        var solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(solverScope, selector);
        assertAllCodesOfIterator(selector.listIterator(), "A");

        // V2 is reachable by A and B
        valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v2);
        selector = new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, false, false);
        solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(solverScope, selector);
        assertAllCodesOfIterator(selector.listIterator(), "A", "B");

        // V3 is reachable by B and C
        valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v3);
        selector = new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, false, false);
        solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(solverScope, selector);
        assertAllCodesOfIterator(selector.listIterator(), "B", "C");

        // V4 is only reachable by C
        valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v4);
        selector = new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, false, false);
        solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(solverScope, selector);
        assertAllCodesOfIterator(selector.listIterator(), "C");

        // V5 is only reachable by C
        valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v5);
        selector = new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, false, false);
        solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(solverScope, selector);
        assertAllCodesOfIterator(selector.listIterator(), "C");

        // Getting the previous element
        valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v3);
        selector = new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, false, false);
        solverScope = solvingStarted(selector, scoreDirector);
        phaseStarted(solverScope, selector);
        var listIterator = selector.listIterator();
        assertThat(listIterator.hasNext()).isTrue();
        assertThat(listIterator.next()).isSameAs(b);
        assertThat(listIterator.hasNext()).isTrue();
        assertThat(listIterator.next()).isSameAs(c);
        assertThat(listIterator.hasNext()).isFalse();
        assertThat(listIterator.hasPrevious()).isTrue();
        assertThat(listIterator.previous()).isSameAs(c);
        assertThat(listIterator.hasPrevious()).isTrue();
        assertThat(listIterator.previous()).isSameAs(b);
    }

    @Test
    void randomWithEntityValueRange() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        // Value order: [v3, v1, v2, v4, v5]
        // Entity order: [a, b, c]
        // Initial state:
        // - 1 [A]
        // - 2 [A, B]
        // - 3 [B, C]
        // - 4 [C]
        // - 5 [C]

        // select C for V3 and first unpinned pos C[0]
        // Random values
        // 1 - pick entity C in RandomFilteringValueRangeIterator
        // 1 - pick random value in ElementPositionRandomIterator and return the first unpinned position
        // 1 - remaining call
        var valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v3);
        var filteringValueRangeSelector = mockIterableFromEntityPropertyValueSelector(valueSelector, true);
        var replayinValueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v3);
        checkEntityValueRange(new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, true, false),
                new FilteringValueRangeSelector<>(filteringValueRangeSelector, replayinValueSelector, true, false),
                scoreDirector, new TestRandom(1, 1, 1), "C[0]");

        // select A for V1 and random pos A[2]
        // Random values
        // 3 - pick a random value in ElementPositionRandomIterator and force generating a random position
        // 0 - pick entity A in RandomFilteringValueRangeIterator
        // 0 - pick random position, only v2 is reachable
        // 0 - remaining call
        valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v1);
        filteringValueRangeSelector = mockIterableFromEntityPropertyValueSelector(valueSelector, true);
        replayinValueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v1);
        // Cause the value iterator return no value at the second call
        doReturn(List.of(v1).iterator(), Collections.emptyIterator()).when(valueSelector).iterator();
        checkEntityValueRange(new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, true, false),
                new FilteringValueRangeSelector<>(filteringValueRangeSelector, replayinValueSelector, true, false),
                scoreDirector, new TestRandom(3, 0, 0, 0), "A[2]");

        // select B for V1 and random pos B[1]
        // 3 - pick a random value in ElementPositionRandomIterator and force generating a random position
        // 1 - pick entity B in RandomFilteringValueRangeIterator
        // 1 - pick random position, v1 and v3 are reachable
        // 0 - remaining call
        valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v2, v2, v2, v2, v2); // simulate five positions
        filteringValueRangeSelector = mockIterableFromEntityPropertyValueSelector(valueSelector, true);
        replayinValueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v2);
        // Cause the value iterator return no value at the second call
        doReturn(List.of(v2).iterator(), Collections.emptyIterator()).when(valueSelector).iterator();
        checkEntityValueRange(new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, true, false),
                new FilteringValueRangeSelector<>(filteringValueRangeSelector, replayinValueSelector, true, false),
                scoreDirector, new TestRandom(3, 1, 1, 0), "B[1]");

        // select C for V5 and first unpinned pos C[1]
        // 3 - pick random value in ElementPositionRandomIterator and force generating a random position
        // 1 - pick random position, v3 and v4 are reachable
        // 0 - remaining call
        valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v5, v5, v5, v5, v5); // simulate five positions
        filteringValueRangeSelector = mockIterableFromEntityPropertyValueSelector(valueSelector, true);
        replayinValueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v5);
        // Cause the value iterator return no value at the second call
        doReturn(List.of(v5).iterator(), Collections.emptyIterator()).when(valueSelector).iterator();
        checkEntityValueRange(new FilteringEntityByValueSelector<>(mockEntitySelector(a, b, c), valueSelector, true, false),
                new FilteringValueRangeSelector<>(filteringValueRangeSelector, replayinValueSelector, true, false),
                scoreDirector, new TestRandom(3, 1, 0), "C[1]");
    }

    private void checkEntityValueRange(FilteringEntityByValueSelector<TestdataListEntityProvidingSolution> entitySelector,
            FilteringValueRangeSelector<TestdataListEntityProvidingSolution> valueSelector,
            InnerScoreDirector<TestdataListEntityProvidingSolution, ?> scoreDirector, TestRandom random, String code) {
        var selector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);
        var solverScope = solvingStarted(selector, scoreDirector, random, entitySelector, valueSelector);
        phaseStarted(solverScope, selector);
        assertCodesOfNeverEndingIterableSelectorWithoutSize(selector, code);
    }

    @Test
    void randomPartiallyPinnedAndUnassigned() {
        var v1 = new TestdataPinnedUnassignedValuesListValue("1");
        var v2 = new TestdataPinnedUnassignedValuesListValue("2");
        var v3 = new TestdataPinnedUnassignedValuesListValue("3");
        var v4 = new TestdataPinnedUnassignedValuesListValue("4");
        var v5 = new TestdataPinnedUnassignedValuesListValue("5");
        var v6 = new TestdataPinnedUnassignedValuesListValue("6");
        var unassignedValue = new TestdataPinnedUnassignedValuesListValue("7");
        var a = new TestdataPinnedUnassignedValuesListEntity("A", v1, v2);
        var b = new TestdataPinnedUnassignedValuesListEntity("B");
        var c = new TestdataPinnedUnassignedValuesListEntity("C", v3);
        var d = new TestdataPinnedUnassignedValuesListEntity("D", v4, v5, v6);
        a.setPlanningPinToIndex(1);
        c.setPlanningPinToIndex(1);
        d.setPlanningPinToIndex(2);

        var solution = new TestdataPinnedUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b, c, d));
        solution.setValueList(List.of(v1, v2, v3, v3, v4, v5, v6, unassignedValue));
        SolutionManager.updateShadowVariables(solution);

        var random = new TestRandom(
                0, // Unassigned element goes first.
                1, // DestinationSelector: First entity
                0, // EntitySelector: A
                2, // DestinationSelector: Second entity
                1, // EntitySelector: B
                3, // DestinationSelector: Third entity
                2, // EntitySelector: C
                4, // DestinationSelector: Fourth entity
                3, // EntitySelector: D
                5, // DestinationSelector: First value
                6, // Destination Selector: Second value
                7, // Destination Selector: Third value; doesn't exist
                3, // EntitySelector: D
                0, // First unpinned position on D
                8, // Destination Selector: Fourth value; doesn't exist
                3, // EntitySelector: D
                1, // Second unpinned position on D
                -1); // (not tested)

        var solutionDescriptor = TestdataPinnedUnassignedValuesListSolution.buildSolutionDescriptor();
        var scoreDirector = mockScoreDirector(TestdataPinnedUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solverScope = new SolverScope<TestdataPinnedUnassignedValuesListSolution>();
        solverScope.setScoreDirector(scoreDirector);
        solverScope.setWorkingRandom(random);
        var entitySelector = new FromSolutionEntitySelector<>(
                solutionDescriptor.findEntityDescriptorOrFail(TestdataPinnedUnassignedValuesListEntity.class),
                SelectionCacheType.PHASE, true);
        entitySelector.solvingStarted(solverScope);
        entitySelector.phaseStarted(new LocalSearchPhaseScope<>(solverScope, 0));

        var valueSelector = mockIterableValueSelector(
                getPinnedAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                unassignedValue, v6, v5, v4, v3, v2, v1);
        var selector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);
        selector.solvingStarted(solverScope);
        selector.phaseStarted(new LocalSearchPhaseScope<>(solverScope, 0));

        assertCodesOfNeverEndingIterator(selector.iterator(),
                "UnassignedLocation",
                "A[1]",
                "B[0]",
                "C[1]",
                "D[2]",
                "D[3]",
                "A[2]",
                "D[2]",
                "D[3]");
    }

    @Test
    void randomUnassignedSingleEntity() {
        var unassignedValue = new TestdataAllowsUnassignedValuesListValue("3");
        var a = new TestdataAllowsUnassignedValuesListEntity("A");

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a));
        solution.setValueList(List.of(unassignedValue));
        SolutionManager.updateShadowVariables(solution);

        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var scoreDirector = mockScoreDirector(solutionDescriptor);
        scoreDirector.setWorkingSolution(solution);

        var solverScope = new SolverScope<TestdataAllowsUnassignedValuesListSolution>();
        solverScope.setScoreDirector(scoreDirector);
        // This needs to use a real Random instance, otherwise the test never covers the situation where
        // the random value of 1 needs to be produced to get to the entity.
        solverScope.setWorkingRandom(new Random(0));
        var entitySelector = new FromSolutionEntitySelector<>(
                solutionDescriptor.findEntityDescriptorOrFail(TestdataAllowsUnassignedValuesListEntity.class),
                SelectionCacheType.PHASE, true);
        entitySelector.solvingStarted(solverScope);
        entitySelector.phaseStarted(new LocalSearchPhaseScope<>(solverScope, 0));

        var valueSelector = mockIterableValueSelector(
                getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                unassignedValue);
        var selector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);
        selector.solvingStarted(solverScope);
        selector.phaseStarted(new LocalSearchPhaseScope<>(solverScope, 0));

        assertCodesOfNeverEndingIterator(selector.iterator(),
                "A[0]", // Ensure the entity is accessible.
                "UnassignedLocation"); // As well as the unassigned location.
    }

    @Test
    void randomFullyPinned() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var a = new TestdataPinnedWithIndexListEntity("A", v1, v2);
        var b = new TestdataPinnedWithIndexListEntity("B");
        var c = new TestdataPinnedWithIndexListEntity("C", v3);
        a.setPlanningPinToIndex(2);
        c.setPinned(true);

        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b);
        var valueSelector =
                TestdataListUtils.mockNeverEndingIterableValueSelector(getPinnedListVariableDescriptor(scoreDirector), v3, v2,
                        v1);
        var selector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);

        // <4 => entity selector; >=4 => value selector
        var random = new TestRandom(
                0, // => A[2]
                4, // => v3 => B[0]
                -1); // (not tested)

        var solverScope = new SolverScope<TestdataPinnedWithIndexListSolution>();
        solverScope.setScoreDirector(scoreDirector);
        solverScope.setWorkingRandom(random);
        selector.solvingStarted(solverScope);
        selector.phaseStarted(new LocalSearchPhaseScope<>(solverScope, 0));

        // Initial state:
        // - A [1, 2]
        // - B []
        // - C [3]

        // The destinations (A[0], C[1], ...) depend on the random number, which decides whether entitySelector or valueSelector
        // will be used; and then on the order of entities and values in the mocked selectors.
        assertAllCodesOfIterator(selector.iterator(),
                "A[2]",
                "B[0]");
    }

    @Test
    void emptyIfThereAreNoEntities() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var solution = new TestdataListSolution();
        solution.setEntityList(Collections.emptyList());
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(new TestdataListEntity[0]);
        var valueSelector =
                mockIterableValueSelector(TestdataListEntity.buildVariableDescriptorForValueList(), v1, v2, v3);

        var randomSelector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);
        solvingStarted(randomSelector, scoreDirector);
        assertEmptyNeverEndingIterableSelector(randomSelector, 0);

        var originalSelector = new ElementDestinationSelector<>(entitySelector, valueSelector, false);
        assertAllCodesOfIterableSelector(originalSelector, 0);
    }

    @Test
    void notEmptyIfThereAreEntities() {
        var a = new TestdataListEntity("A");
        var b = new TestdataListEntity("B");
        var v1 = new TestdataListValue("1");
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b);
        var valueSelector = mockIterableValueSelector(getListVariableDescriptor(scoreDirector));

        var originalSelector = new ElementDestinationSelector<>(entitySelector, valueSelector, false);
        assertAllCodesOfIterableSelector(originalSelector, 2, "A[0]", "B[0]");

        var randomSelector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);
        var random = new TestRandom(0, 1);

        solvingStarted(randomSelector, scoreDirector, random);
        // Do not assert all codes to prevent exhausting the iterator.
        assertCodesOfNeverEndingIterableSelector(randomSelector, 2, "A[0]");
    }

    @Test
    void notEmptyIfThereAreEntitiesWithPinning() {
        var a = new TestdataPinnedWithIndexListEntity("A");
        var b = new TestdataPinnedWithIndexListEntity("B", new TestdataPinnedWithIndexListValue("B0"),
                new TestdataPinnedWithIndexListValue("B1"));
        b.setPlanningPinToIndex(1); // B0 will be ignored.
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(b.getValueList().get(0), b.getValueList().get(1)));
        SolutionManager.updateShadowVariables(solution);

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b);
        var valueSelector = mockIterableValueSelector(getPinnedListVariableDescriptor(scoreDirector));

        var originalSelector = new ElementDestinationSelector<>(entitySelector, valueSelector, false);
        assertAllCodesOfIterableSelector(originalSelector, 2, "A[0]", "B[1]");

        var randomSelector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);
        var random = new TestRandom(0, 1);

        solvingStarted(randomSelector, scoreDirector, random);
        // Do not assert all codes to prevent exhausting the iterator.
        assertCodesOfNeverEndingIterableSelector(randomSelector, 2, "A[0]");
    }

    @Test
    void phaseLifecycle() {
        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var entitySelector = mockEntitySelector(new TestdataListEntity[0]);
        var valueSelector = mockIterableValueSelector(getListVariableDescriptor(scoreDirector));

        var selector = new ElementDestinationSelector<>(entitySelector, valueSelector, false);

        var solverScope = solvingStarted(selector, scoreDirector);
        var phaseScope = phaseStarted(selector, solverScope);

        var stepScope1 = stepStarted(selector, phaseScope);
        selector.stepEnded(stepScope1);

        var stepScope2 = stepStarted(selector, phaseScope);
        selector.stepEnded(stepScope2);

        selector.phaseEnded(phaseScope);
        selector.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 1, 2);
        verifyPhaseLifecycle(valueSelector, 1, 1, 2);
    }

    @Test
    void discardOldValues() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b, c);
        var entityIterator = mock(UpcomingSelectionIterator.class);
        doReturn(entityIterator).when(entitySelector).iterator();
        doReturn(a).when(entityIterator).next();
        doReturn(true, true, false).when(entityIterator).hasNext();
        var valueSelector = mockIterableValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v3, v3);
        IterableValueSelector<TestdataListEntityProvidingSolution> replayingValueIterator =
                mockReplayingValueSelector(getEntityRangeListVariableDescriptor(scoreDirector), v1, v3);

        var selector = new ElementDestinationSelector<>(entitySelector, replayingValueIterator, valueSelector, true, false);

        // <4 => entity selector; >=4 => value selector
        // Picks value selector twice
        var random = new TestRandom(5, 5, 5, 5);

        solvingStarted(selector, scoreDirector, random);
        assertAllCodesOfIterator(selector.iterator(), "B[1]", "B[1]");

        // Even using only the value selector, the entity iterator must discard previous during the hasNext() calls
        verify(entityIterator, times(1)).next();
    }
}
