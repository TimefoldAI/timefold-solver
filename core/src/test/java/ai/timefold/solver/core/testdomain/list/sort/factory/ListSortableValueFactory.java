package ai.timefold.solver.core.testdomain.list.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterWeightFactory;

public class ListSortableValueFactory
        implements SorterWeightFactory<TestdataListFactorySortableSolution, TestdataListFactorySortableValue> {

    @Override
    public Comparable<TestdataListFactorySortableValue> createSorterWeight(TestdataListFactorySortableSolution solution,
            TestdataListFactorySortableValue selection) {
        return selection;
    }
}
