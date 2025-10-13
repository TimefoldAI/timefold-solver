package ai.timefold.solver.core.testdomain.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterFactory;

public class SortableEntityFactory
        implements SorterFactory<TestdataFactorySortableSolution, TestdataFactorySortableEntity> {

    @Override
    public Comparable<TestdataFactorySortableEntity> createSorter(TestdataFactorySortableSolution solution,
                                                                  TestdataFactorySortableEntity selection) {
        return selection;
    }
}
