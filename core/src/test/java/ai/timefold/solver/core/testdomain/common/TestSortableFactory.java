package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class TestSortableFactory
        implements SelectionSorterWeightFactory<Object, TestSortableObject> {

    @Override
    public Comparable createSorterWeight(Object o, TestSortableObject selection) {
        return selection;
    }
}
