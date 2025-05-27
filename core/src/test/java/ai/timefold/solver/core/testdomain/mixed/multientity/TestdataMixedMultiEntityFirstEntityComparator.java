package ai.timefold.solver.core.testdomain.mixed.multientity;

import java.util.Comparator;

public class TestdataMixedMultiEntityFirstEntityComparator implements Comparator<TestdataMixedMultiEntityFirstEntity> {
    @Override
    public int compare(TestdataMixedMultiEntityFirstEntity v1, TestdataMixedMultiEntityFirstEntity v2) {
        // ASC sort
        return v1.getDifficulty() - v2.getDifficulty();
    }
}
