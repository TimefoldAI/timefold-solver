package ai.timefold.solver.core.testdomain.common;

public interface TestSortableObject extends Comparable<TestSortableObject> {

    int getComparatorValue();

    @Override
    default int compareTo(TestSortableObject o) {
        return getComparatorValue() - o.getComparatorValue();
    }
}
