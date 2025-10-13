package ai.timefold.solver.core.testdomain.valuerange.sort.factory;

import ai.timefold.solver.core.api.domain.common.SorterFactory;

public class SortableEntityFactory
        implements
        SorterFactory<TestdataFactorySortableEntityProvidingSolution, TestdataFactorySortableEntityProvidingEntity> {

    @Override
    public Comparable<TestdataFactorySortableEntityProvidingEntity> createSorter(
            TestdataFactorySortableEntityProvidingSolution solution,
            TestdataFactorySortableEntityProvidingEntity selection) {
        return selection;
    }
}
