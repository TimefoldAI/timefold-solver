package ai.timefold.solver.core.testdomain.list.valuerange.sort.comparator;

import java.util.Comparator;

public class ListSortableEntityComparator implements Comparator<TestdataListSortableEntityProvidingEntity> {

    @Override
    public int compare(TestdataListSortableEntityProvidingEntity e1, TestdataListSortableEntityProvidingEntity e2) {
        return e1.getDifficulty() - e2.getDifficulty();
    }
}
