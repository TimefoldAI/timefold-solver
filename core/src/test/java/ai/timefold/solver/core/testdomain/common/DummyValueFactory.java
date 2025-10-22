package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.sort.factory.TestdataListFactorySortableSolution;

public class DummyValueFactory
        implements ComparatorFactory<TestdataListFactorySortableSolution, TestdataValue, Integer>,
        SelectionSorterWeightFactory<TestdataListFactorySortableSolution, TestdataValue> {

    @Override
    public Integer createSorter(TestdataListFactorySortableSolution solution, TestdataValue selection) {
        return 0;
    }

    @Override
    public Comparable createSorterWeight(TestdataListFactorySortableSolution solution, TestdataValue selection) {
        return createSorter(solution, selection);
    }
}
