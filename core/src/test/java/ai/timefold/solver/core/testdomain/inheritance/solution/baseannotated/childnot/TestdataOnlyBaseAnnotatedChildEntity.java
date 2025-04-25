package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childnot;

import ai.timefold.solver.core.testdomain.TestdataValue;

public class TestdataOnlyBaseAnnotatedChildEntity extends TestdataOnlyBaseAnnotatedBaseEntity {

    private Object extraObject;

    public TestdataOnlyBaseAnnotatedChildEntity() {
    }

    public TestdataOnlyBaseAnnotatedChildEntity(String code) {
        super(code);
    }

    public TestdataOnlyBaseAnnotatedChildEntity(String code, TestdataValue value) {
        super(code, value);
    }

    public TestdataOnlyBaseAnnotatedChildEntity(String code, TestdataValue value, Object extraObject) {
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
