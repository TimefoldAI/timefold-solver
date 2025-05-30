package ai.timefold.solver.core.testdomain.mixed.singleentity;

import java.util.Comparator;

public class TestdataMixedOtherValueComparator implements Comparator<TestdataMixedOtherValue> {
    @Override
    public int compare(TestdataMixedOtherValue o1, TestdataMixedOtherValue o2) {
        return o1.getStrength() - o2.getStrength();
    }
}
