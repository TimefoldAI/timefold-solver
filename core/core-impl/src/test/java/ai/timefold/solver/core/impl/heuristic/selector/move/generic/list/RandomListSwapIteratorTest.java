package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfIterator;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class RandomListSwapIteratorTest {

    @Test
    void iterator() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        var e1 = TestdataListEntity.createWithValues("A", v1, v2);
        var e2 = TestdataListEntity.createWithValues("B");
        var e3 = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2, e3));
        solution.setValueList(List.of(v1, v2, v3));

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        RandomListSwapIterator<TestdataListSolution> randomListSwapIterator = new RandomListSwapIterator<>(
                scoreDirector.getSupplyManager().demand(listVariableDescriptor.getStateDemand()),
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v1, v1, v3),
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v2, v3, v1));

        assertCodesOfIterator(randomListSwapIterator,
                "No change",
                "1 {A[0]} <-> 2 {A[1]}",
                "1 {A[0]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 1 {A[0]}");
    }
}
