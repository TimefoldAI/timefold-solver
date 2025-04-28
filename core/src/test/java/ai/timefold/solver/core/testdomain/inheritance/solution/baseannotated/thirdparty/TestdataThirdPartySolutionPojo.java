package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.thirdparty;

import java.util.List;

import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

/**
 * This POJO does not depend on Timefold:
 * it has no Timefold imports (annotations, score, ...) except for test imports.
 */
public class TestdataThirdPartySolutionPojo extends TestdataObject {

    private List<TestdataValue> valueList;
    private List<TestdataThirdPartyEntityPojo> entityList;

    public TestdataThirdPartySolutionPojo() {
    }

    public TestdataThirdPartySolutionPojo(String code) {
        super(code);
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    public List<TestdataThirdPartyEntityPojo> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataThirdPartyEntityPojo> entityList) {
        this.entityList = entityList;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
