package ai.timefold.solver.core.testdomain.common;

import java.util.Comparator;

import ai.timefold.solver.core.testdomain.TestdataValue;

public class DummyValueComparator implements Comparator<TestdataValue> {

    @Override
    public int compare(TestdataValue v1, TestdataValue v2) {
        return 0;
    }
}
