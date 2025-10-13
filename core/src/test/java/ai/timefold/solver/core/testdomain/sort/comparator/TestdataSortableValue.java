package ai.timefold.solver.core.testdomain.sort.comparator;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataSortableValue extends TestdataObject {

    private int strength;

    public TestdataSortableValue() {
    }

    public TestdataSortableValue(String code, int strength) {
        super(code);
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

}
