package ai.timefold.solver.core.testdomain.list.valuerange.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterFactory;

public class ListSortableValueFactory
        implements
        SorterFactory<TestdataListFactorySortableEntityProvidingSolution, TestdataListFactorySortableEntityProvidingValue> {

    @Override
    public Comparable<TestdataListFactorySortableEntityProvidingValue> createSorter(
            TestdataListFactorySortableEntityProvidingSolution solution,
            TestdataListFactorySortableEntityProvidingValue selection) {
        return selection;
    }
}
