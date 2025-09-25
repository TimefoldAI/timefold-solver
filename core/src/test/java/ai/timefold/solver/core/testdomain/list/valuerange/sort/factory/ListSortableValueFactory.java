package ai.timefold.solver.core.testdomain.list.valuerange.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterWeightFactory;

public class ListSortableValueFactory
        implements
        SorterWeightFactory<TestdataListFactorySortableEntityProvidingSolution, TestdataListFactorySortableEntityProvidingValue> {

    @Override
    public Comparable<TestdataListFactorySortableEntityProvidingValue> createSorterWeight(
            TestdataListFactorySortableEntityProvidingSolution solution,
            TestdataListFactorySortableEntityProvidingValue selection) {
        return selection;
    }
}
