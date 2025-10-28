package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataSortableValue extends TestdataObject implements TestSortableObject {

    private int strength;

    public TestdataSortableValue() {
    }

    public TestdataSortableValue(String code, int strength) {
        super(code);
        this.strength = strength;
    }

    @Override
    public int getComparatorValue() {
        return strength;
    }
}
