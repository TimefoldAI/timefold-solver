package ai.timefold.solver.core.testdomain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.testutil.CodeAssertable;

public class TestdataObject implements CodeAssertable {

    @PlanningId
    protected String code;

    public TestdataObject() {
    }

    public TestdataObject(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }

}
