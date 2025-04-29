package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataMultipleBothAnnotatedChildEntity extends TestdataMultipleBothAnnotatedSecondChildEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange2")
    private String value2;

    public TestdataMultipleBothAnnotatedChildEntity() {
    }

    public TestdataMultipleBothAnnotatedChildEntity(long id) {
        super(id);
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }
}
