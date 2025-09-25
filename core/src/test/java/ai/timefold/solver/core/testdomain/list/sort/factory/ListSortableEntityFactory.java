package ai.timefold.solver.core.testdomain.list.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterWeightFactory;

public class ListSortableEntityFactory
        implements SorterWeightFactory<TestdataListFactorySortableSolution, TestdataListFactorySortableEntity> {

    @Override
    public Comparable<TestdataListFactorySortableEntity> createSorterWeight(TestdataListFactorySortableSolution solution,
            TestdataListFactorySortableEntity selection) {
        return selection;
    }
}
