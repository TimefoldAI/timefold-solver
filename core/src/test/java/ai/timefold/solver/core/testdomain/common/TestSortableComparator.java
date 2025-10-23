package ai.timefold.solver.core.testdomain.common;

import java.util.Comparator;

public class TestSortableComparator implements Comparator<TestSortableObject> {

    @Override
    public int compare(TestSortableObject v1, TestSortableObject v2) {
        return v1.getComparatorValue() - v2.getComparatorValue();
    }
}
