package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.thirdparty;

import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

/**
 * This POJO does not depend on Timefold:
 * it has no Timefold imports (annotations, score, ...) except for test imports.
 */
public class TestdataThirdPartyEntityPojo extends TestdataObject {

    private TestdataValue value;

    public TestdataThirdPartyEntityPojo() {
    }

    public TestdataThirdPartyEntityPojo(String code) {
        super(code);
    }

    public TestdataThirdPartyEntityPojo(String code, TestdataValue value) {
        this(code);
        this.value = value;
    }

    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
