package ai.timefold.solver.core.testdomain.sort.factory;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class SortableValueFactory
        implements SelectionSorterWeightFactory<TestdataFactorySortableSolution, TestdataFactorySortableValue> {

    @Override
    public Comparable createSorterWeight(TestdataFactorySortableSolution solution, TestdataFactorySortableValue selection) {
        return selection;
    }

}
