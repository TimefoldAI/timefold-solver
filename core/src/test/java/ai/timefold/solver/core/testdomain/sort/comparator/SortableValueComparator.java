package ai.timefold.solver.core.testdomain.sort.comparator;

import java.util.Comparator;

public class SortableValueComparator implements Comparator<TestdataSortableValue> {

    @Override
    public int compare(TestdataSortableValue v1, TestdataSortableValue v2) {
        return v1.getStrength() - v2.getStrength();
    }
}
