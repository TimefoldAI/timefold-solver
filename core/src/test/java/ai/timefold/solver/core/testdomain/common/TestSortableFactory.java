package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.api.domain.common.SorterFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class TestSortableFactory
        implements SelectionSorterWeightFactory<Object, TestSortableObject>, SorterFactory<Object, TestSortableObject> {
    @Override
    public Comparable createSorterWeight(Object o, TestSortableObject selection) {
        return selection;
    }

    @Override
    public Comparable createSorter(Object o, TestSortableObject selection) {
        return selection;
    }
}
