package ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataUnassignedMixedValue extends TestdataObject {

    private boolean blocked = false;

    public TestdataUnassignedMixedValue() {
        // Required for cloner
    }

    public TestdataUnassignedMixedValue(String code) {
        super(code);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
