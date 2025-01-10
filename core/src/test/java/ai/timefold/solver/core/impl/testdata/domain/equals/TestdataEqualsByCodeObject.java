package ai.timefold.solver.core.impl.testdata.domain.equals;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.impl.testdata.util.CodeAssertable;

public class TestdataEqualsByCodeObject implements CodeAssertable {

    @PlanningId
    protected String code;

    public TestdataEqualsByCodeObject() {
    }

    public TestdataEqualsByCodeObject(String code) {
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

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o.getClass() != this.getClass()) {
            return false;
        } else {
            var that = (TestdataEqualsByCodeObject) o;
            return Objects.equals(code, that.code);
        }
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
