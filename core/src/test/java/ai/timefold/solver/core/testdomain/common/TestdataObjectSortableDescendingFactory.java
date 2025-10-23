package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataObjectSortableDescendingFactory implements SelectionSorterWeightFactory<Object, TestdataObject> {

    @Override
    public Comparable createSorterWeight(Object solution, TestdataObject selection) {
        // Descending order
        return -extractCode(selection.getCode());
    }

    public static int extractCode(String code) {
        var idx = code.lastIndexOf(" ");
        return Integer.parseInt(code.substring(idx + 1));
    }
}
