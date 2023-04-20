package ai.timefold.solver.core.impl.testdata.domain.extended.thirdparty;

import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

/**
 * This POJO does not depend on OptaPlanner:
 * it has no OptaPlanner imports (annotations, score, ...) except for test imports.
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
