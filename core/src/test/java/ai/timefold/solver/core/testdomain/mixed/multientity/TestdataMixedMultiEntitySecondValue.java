package ai.timefold.solver.core.testdomain.mixed.multientity;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataMixedMultiEntitySecondValue extends TestdataObject {

    private int strength;

    public TestdataMixedMultiEntitySecondValue() {
        // Required for cloner
    }

    public TestdataMixedMultiEntitySecondValue(String code, int strength) {
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
