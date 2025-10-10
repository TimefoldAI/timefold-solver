package ai.timefold.solver.core.testdomain.list.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterFactory;

public class ListSortableValueFactory
        implements SorterFactory<TestdataListFactorySortableSolution, TestdataListFactorySortableValue> {

    @Override
    public Comparable<TestdataListFactorySortableValue> createSorter(TestdataListFactorySortableSolution solution,
            TestdataListFactorySortableValue selection) {
        return selection;
    }
}
