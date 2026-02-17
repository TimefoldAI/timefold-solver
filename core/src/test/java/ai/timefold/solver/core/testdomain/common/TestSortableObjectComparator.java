package ai.timefold.solver.core.testdomain.common;

import java.util.Comparator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestSortableObjectComparator implements Comparator<TestSortableObject> {

    @Override
    public int compare(TestSortableObject v1, TestSortableObject v2) {
        return v1.getComparatorValue() - v2.getComparatorValue();
    }
}
