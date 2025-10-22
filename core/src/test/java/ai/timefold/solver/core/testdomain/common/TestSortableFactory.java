package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class TestSortableFactory
        implements SelectionSorterWeightFactory<Object, TestSortableObject>,
        ComparatorFactory<Object, TestSortableObject, TestSortableObject> {
    @Override
    public Comparable createSorterWeight(Object o, TestSortableObject selection) {
        return selection;
    }

    @Override
    public TestSortableObject createSorter(Object o, TestSortableObject selection) {
        return selection;
    }
}
