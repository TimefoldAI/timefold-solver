package ai.timefold.solver.core.testdomain.multivar.list.singleentity.unassignedvar;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataUnassignedListMultiVarOtherValue extends TestdataObject {

    private boolean blocked = false;

    public TestdataUnassignedListMultiVarOtherValue() {
        // Required for cloner
    }

    public TestdataUnassignedListMultiVarOtherValue(String code) {
        super(code);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
