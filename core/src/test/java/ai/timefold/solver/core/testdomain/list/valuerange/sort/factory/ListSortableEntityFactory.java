package ai.timefold.solver.core.testdomain.list.valuerange.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterWeightFactory;

public class ListSortableEntityFactory
        implements
        SorterWeightFactory<TestdataListFactorySortableEntityProvidingSolution, TestdataListFactorySortableEntityProvidingEntity> {

    @Override
    public Comparable<TestdataListFactorySortableEntityProvidingEntity> createSorterWeight(
            TestdataListFactorySortableEntityProvidingSolution solution,
            TestdataListFactorySortableEntityProvidingEntity selection) {
        return selection;
    }
}
