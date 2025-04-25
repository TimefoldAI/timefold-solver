package ai.timefold.solver.core.testdomain.comparable;

import java.util.Comparator;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataCodeComparator implements Comparator<TestdataObject> {

    private static final Comparator<TestdataObject> COMPARATOR = Comparator.comparing(TestdataObject::getCode);

    @Override
    public int compare(TestdataObject a, TestdataObject b) {
        return COMPARATOR.compare(a, b);
    }
}
