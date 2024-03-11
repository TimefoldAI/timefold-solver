package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntitySelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfIterator;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import java.util.List;

import ai.timefold.solver.core.impl.heuristic.selector.list.ElementDestinationSelector;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class RandomListChangeIteratorTest {

    @Test
    void iterator() {
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

        var listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        // Iterates over values in this given order.
        var sourceValueSelector =
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v2, v3);
        var destinationValueSelector =
                mockEntityIndependentValueSelector(listVariableDescriptor, v2, v3);
        var entitySelector = mockEntitySelector(b, a, c);
        var destinationSelector = new ElementDestinationSelector<>(entitySelector, destinationValueSelector, true);

        var random = new TestRandom(3, 0, 1);
        solvingStarted(destinationSelector, scoreDirector, random);
        var randomListChangeIterator = new RandomListChangeIterator<>(
                scoreDirector.getSupplyManager().demand(listVariableDescriptor.getStateDemand()),
                sourceValueSelector,
                destinationSelector);

        // <3 => entity selector; >=3 => value selector
        final var destinationRange = entitySelector.getSize() + destinationValueSelector.getSize();

        // The moved values (1, 2, 3) and their source positions are supplied by the mocked value selector.
        // The test is focused on the destinations (A[2], B[0], A[0]), which reflect the numbers supplied by the test random.
        assertCodesOfIterator(randomListChangeIterator,
                "1 {A[0]->A[2]}",
                "2 {A[1]->B[0]}",
                "3 {C[0]->A[0]}");

        random.assertIntBoundJustRequested((int) destinationRange);
    }
}
