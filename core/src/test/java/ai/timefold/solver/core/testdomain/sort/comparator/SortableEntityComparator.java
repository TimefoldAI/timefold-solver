package ai.timefold.solver.core.testdomain.sort.comparator;

import java.util.Comparator;

public class SortableEntityComparator implements Comparator<TestdataSortableEntity> {

    @Override
    public int compare(TestdataSortableEntity e1, TestdataSortableEntity e2) {
        return e1.getDifficulty() - e2.getDifficulty();
    }
}
