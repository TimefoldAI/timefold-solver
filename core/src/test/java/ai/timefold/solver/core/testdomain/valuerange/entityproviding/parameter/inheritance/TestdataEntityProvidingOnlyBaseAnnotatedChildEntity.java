package ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.inheritance;

import ai.timefold.solver.core.testdomain.TestdataValue;

public class TestdataEntityProvidingOnlyBaseAnnotatedChildEntity extends TestdataEntityProvidingOnlyBaseAnnotatedBaseEntity {

    private Object extraObject;

    public TestdataEntityProvidingOnlyBaseAnnotatedChildEntity() {
    }

    public TestdataEntityProvidingOnlyBaseAnnotatedChildEntity(String code) {
        super(code);
    }

    public TestdataEntityProvidingOnlyBaseAnnotatedChildEntity(String code, TestdataValue value) {
        super(code, value);
    }

    public TestdataEntityProvidingOnlyBaseAnnotatedChildEntity(String code, TestdataValue value, Object extraObject) {
        super(code, value);
        this.extraObject = extraObject;
    }

    public Object getExtraObject() {
        return extraObject;
    }

    public void setExtraObject(Object extraObject) {
        this.extraObject = extraObject;
    }
}
