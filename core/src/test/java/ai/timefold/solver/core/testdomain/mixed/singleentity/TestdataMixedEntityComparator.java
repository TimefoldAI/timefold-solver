package ai.timefold.solver.core.testdomain.mixed.singleentity;

import java.util.Comparator;

public class TestdataMixedEntityComparator implements Comparator<TestdataMixedEntity> {
    @Override
    public int compare(TestdataMixedEntity v1, TestdataMixedEntity v2) {
        return v1.getDifficulty() - v2.getDifficulty();
    }
}
