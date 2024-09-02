package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.stepStarted;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getPinnedAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntitySelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockNeverEndingEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfIterableSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingIterableSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingIterator;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertEmptyNeverEndingIterableSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.entity.FromSolutionEntitySelector;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class ElementDestinationSelectorTest {

    @Test
    void original() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var a = TestdataListEntity.createWithValues("A", v2, v1);
        var b = TestdataListEntity.createWithValues("B");
        var c = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b, c);
        var valueSelector = mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v3, v1, v2);
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
        var a = TestdataListEntity.createWithValues("A", v1, v2);
        var b = TestdataListEntity.createWithValues("B");
        var c = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b, c, c);
        var valueSelector = mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v3, v2, v1);
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
    void randomPartiallyPinnedAndUnassigned() {
        var v1 = new TestdataPinnedUnassignedValuesListValue("1");
        var v2 = new TestdataPinnedUnassignedValuesListValue("2");
        var v3 = new TestdataPinnedUnassignedValuesListValue("3");
        var v4 = new TestdataPinnedUnassignedValuesListValue("4");
        var v5 = new TestdataPinnedUnassignedValuesListValue("5");
        var v6 = new TestdataPinnedUnassignedValuesListValue("5");
        var unassignedValue = new TestdataPinnedUnassignedValuesListValue("7");
        var a = TestdataPinnedUnassignedValuesListEntity.createWithValues("A", v1, v2);
        var b = TestdataPinnedUnassignedValuesListEntity.createWithValues("B");
        var c = TestdataPinnedUnassignedValuesListEntity.createWithValues("C", v3);
        var d = TestdataPinnedUnassignedValuesListEntity.createWithValues("D", v4, v5, v6);
        a.setPlanningPinToIndex(1);
        c.setPlanningPinToIndex(1);
        d.setPlanningPinToIndex(2);

        var solution = new TestdataPinnedUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b, c, d));
        solution.setValueList(List.of(v1, v2, v3, v3, v4, v5, unassignedValue));

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

        var valueSelector = mockEntityIndependentValueSelector(
                getPinnedAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                unassignedValue, v6, v5, v4, v3, v2, v1);
        var selector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);
        selector.solvingStarted(solverScope);
        selector.phaseStarted(new LocalSearchPhaseScope<>(solverScope, 0));

        assertCodesOfNeverEndingIterator(selector.iterator(),
                "UnassignedLocation[]",
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
    void randomFullyPinned() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v1, v2);
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B");
        var c = TestdataPinnedWithIndexListEntity.createWithValues("C", v3);
        a.setPlanningPinToIndex(2);
        c.setPinned(true);

        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b);
        var valueSelector =
                mockNeverEndingEntityIndependentValueSelector(getPinnedListVariableDescriptor(scoreDirector), v3, v2, v1);
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
                mockEntityIndependentValueSelector(TestdataListEntity.buildVariableDescriptorForValueList(), v1, v2, v3);

        var randomSelector = new ElementDestinationSelector<>(entitySelector, valueSelector, true);
        solvingStarted(randomSelector, scoreDirector);
        assertEmptyNeverEndingIterableSelector(randomSelector, 0);

        var originalSelector = new ElementDestinationSelector<>(entitySelector, valueSelector, false);
        assertAllCodesOfIterableSelector(originalSelector, 0);
    }

    @Test
    void notEmptyIfThereAreEntities() {
        var a = TestdataListEntity.createWithValues("A");
        var b = TestdataListEntity.createWithValues("B");
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of());

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b);
        var valueSelector = mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector));

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
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A");
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B",
                new TestdataPinnedWithIndexListValue("B0"), new TestdataPinnedWithIndexListValue("B1"));
        b.setPlanningPinToIndex(1); // B0 will be ignored.
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(b.getValueList().get(0), b.getValueList().get(1)));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var entitySelector = mockEntitySelector(a, b);
        var valueSelector = mockEntityIndependentValueSelector(getPinnedListVariableDescriptor(scoreDirector));

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
        var valueSelector = mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector));

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
}
