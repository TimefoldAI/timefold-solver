package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.sort.factory.TestdataListFactorySortableSolution;

public class DummyWeightValueFactory
        implements SelectionSorterWeightFactory<TestdataListFactorySortableSolution, TestdataValue> {

    @Override
    public Comparable createSorterWeight(TestdataListFactorySortableSolution solution, TestdataValue selection) {
        return v -> 0;
    }
}
