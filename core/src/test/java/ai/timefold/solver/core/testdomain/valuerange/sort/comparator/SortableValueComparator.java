package ai.timefold.solver.core.testdomain.valuerange.sort.comparator;

import java.util.Comparator;

public class SortableValueComparator implements Comparator<TestdataSortableEntityProvidingValue> {

    @Override
    public int compare(TestdataSortableEntityProvidingValue v1, TestdataSortableEntityProvidingValue v2) {
        return v1.getStrength() - v2.getStrength();
    }
}
