package ai.timefold.solver.core.testdomain.valuerange.sort.comparator;

import java.util.Comparator;

public class SortableEntityComparator implements Comparator<TestdataSortableEntityProvidingEntity> {

    @Override
    public int compare(TestdataSortableEntityProvidingEntity e1, TestdataSortableEntityProvidingEntity e2) {
        return e1.getDifficulty() - e2.getDifficulty();
    }
}
