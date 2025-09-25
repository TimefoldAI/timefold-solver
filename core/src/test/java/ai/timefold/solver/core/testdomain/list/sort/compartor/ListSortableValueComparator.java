package ai.timefold.solver.core.testdomain.list.sort.compartor;

import java.util.Comparator;

public class ListSortableValueComparator implements Comparator<TestdataListSortableValue> {

    @Override
    public int compare(TestdataListSortableValue v1, TestdataListSortableValue v2) {
        return v1.getStrength() - v2.getStrength();
    }
}
