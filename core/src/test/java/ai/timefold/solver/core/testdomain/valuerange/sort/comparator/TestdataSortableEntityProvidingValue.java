package ai.timefold.solver.core.testdomain.valuerange.sort.comparator;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataSortableEntityProvidingValue extends TestdataObject {

    private int strength;

    public TestdataSortableEntityProvidingValue() {
    }

    public TestdataSortableEntityProvidingValue(String code, int strength) {
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
