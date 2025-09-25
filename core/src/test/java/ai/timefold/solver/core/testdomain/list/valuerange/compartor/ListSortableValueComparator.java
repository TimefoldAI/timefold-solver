package ai.timefold.solver.core.testdomain.list.valuerange.compartor;

import java.util.Comparator;

public class ListSortableValueComparator implements Comparator<TestdataListSortableEntityProvidingValue> {

    @Override
    public int compare(TestdataListSortableEntityProvidingValue v1, TestdataListSortableEntityProvidingValue v2) {
        return v1.getStrength() - v2.getStrength();
    }
}
