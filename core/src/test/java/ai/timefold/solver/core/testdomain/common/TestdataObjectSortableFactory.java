package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataObjectSortableFactory implements SelectionSorterWeightFactory<Object, TestdataObject>,
        ComparatorFactory<Object, TestdataObject> {

    @Override
    public Comparable createSorterWeight(Object solution, TestdataObject selection) {
        return createSorter(solution, selection);
    }

    @Override
    public Comparable createSorter(Object solution, TestdataObject selection) {
        return -extractCode(selection.getCode());
    }

    public static int extractCode(String code) {
        var idx = code.lastIndexOf(" ");
        return Integer.parseInt(code.substring(idx + 1));
    }
}
