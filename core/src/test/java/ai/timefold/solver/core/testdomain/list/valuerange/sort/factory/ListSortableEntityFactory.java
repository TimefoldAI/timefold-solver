package ai.timefold.solver.core.testdomain.list.valuerange.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterFactory;

public class ListSortableEntityFactory
        implements
        SorterFactory<TestdataListFactorySortableEntityProvidingSolution, TestdataListFactorySortableEntityProvidingEntity> {

    @Override
    public Comparable<TestdataListFactorySortableEntityProvidingEntity> createSorter(
            TestdataListFactorySortableEntityProvidingSolution solution,
            TestdataListFactorySortableEntityProvidingEntity selection) {
        return selection;
    }
}
