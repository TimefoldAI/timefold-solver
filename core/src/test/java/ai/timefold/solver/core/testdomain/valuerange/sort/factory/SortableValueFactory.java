package ai.timefold.solver.core.testdomain.valuerange.sort.factory;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class SortableValueFactory
        implements
        SelectionSorterWeightFactory<TestdataFactorySortableEntityProvidingSolution, TestdataFactorySortableEntityProvidingValue> {

    @Override
    public Comparable createSorterWeight(TestdataFactorySortableEntityProvidingSolution solution,
            TestdataFactorySortableEntityProvidingValue selection) {
        return selection;
    }
}
