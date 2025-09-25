package ai.timefold.solver.core.testdomain.list.sort.compartor;

import java.util.Comparator;

public class ListSortableEntityComparator implements Comparator<TestdataListSortableEntity> {

    @Override
    public int compare(TestdataListSortableEntity e1, TestdataListSortableEntity e2) {
        return e1.getDifficulty() - e2.getDifficulty();
    }
}
