package ai.timefold.solver.core.testdomain.sort.factory;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataFactorySortableValue extends TestdataObject implements Comparable<TestdataFactorySortableValue> {

    private int strength;

    public TestdataFactorySortableValue() {
    }

    public TestdataFactorySortableValue(String code, int strength) {
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
    public int compareTo(TestdataFactorySortableValue o) {
        return strength - o.strength;
    }
}
