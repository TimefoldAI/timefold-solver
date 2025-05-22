package ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataUnassignedMixedOtherValue extends TestdataObject {

    private boolean blocked = false;

    public TestdataUnassignedMixedOtherValue() {
        // Required for cloner
    }

    public TestdataUnassignedMixedOtherValue(String code) {
        super(code);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
