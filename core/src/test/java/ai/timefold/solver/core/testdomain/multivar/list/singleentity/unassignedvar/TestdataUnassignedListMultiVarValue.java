package ai.timefold.solver.core.testdomain.multivar.list.singleentity.unassignedvar;

import ai.timefold.solver.core.testdomain.TestdataObject;

public class TestdataUnassignedListMultiVarValue extends TestdataObject {

    private boolean blocked = false;

    public TestdataUnassignedListMultiVarValue() {
        // Required for cloner
    }

    public TestdataUnassignedListMultiVarValue(String code) {
        super(code);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
