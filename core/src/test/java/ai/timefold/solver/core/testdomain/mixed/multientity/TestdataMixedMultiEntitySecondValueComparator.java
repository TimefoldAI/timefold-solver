package ai.timefold.solver.core.testdomain.mixed.multientity;

import java.util.Comparator;

public class TestdataMixedMultiEntitySecondValueComparator implements Comparator<TestdataMixedMultiEntitySecondValue> {
    @Override
    public int compare(TestdataMixedMultiEntitySecondValue v1, TestdataMixedMultiEntitySecondValue v2) {
        // ASC sort
        return v1.getStrength() - v2.getStrength();
    }
}
