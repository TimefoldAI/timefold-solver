package ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataBaseEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

    public TestdataBaseEntity(String code) {
        super(code);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
