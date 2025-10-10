package ai.timefold.solver.core.testdomain.list.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterFactory;

public class ListSortableEntityFactory
        implements SorterFactory<TestdataListFactorySortableSolution, TestdataListFactorySortableEntity> {

    @Override
    public Comparable<TestdataListFactorySortableEntity> createSorter(TestdataListFactorySortableSolution solution,
            TestdataListFactorySortableEntity selection) {
        return selection;
    }
}
