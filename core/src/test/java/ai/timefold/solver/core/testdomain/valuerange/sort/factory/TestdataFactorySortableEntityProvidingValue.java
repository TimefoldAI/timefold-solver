package ai.timefold.solver.core.testdomain.valuerange.sort.factory;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataFactorySortableEntityProvidingValue extends TestdataObject
        implements Comparable<TestdataFactorySortableEntityProvidingValue> {

    private int strength;

    public TestdataFactorySortableEntityProvidingValue() {
    }

    public TestdataFactorySortableEntityProvidingValue(String code, int strength) {
        super(code);
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    @Override
    public int compareTo(TestdataFactorySortableEntityProvidingValue o) {
        return strength - o.strength;
    }
}
