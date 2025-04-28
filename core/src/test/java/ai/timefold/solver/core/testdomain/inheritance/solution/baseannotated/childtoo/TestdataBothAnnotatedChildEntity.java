package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataBothAnnotatedChildEntity extends TestdataEntity {

    @PlanningVariable(valueRangeProviderRefs = "subValueRange")
    private TestdataValue subValue;

    public TestdataBothAnnotatedChildEntity() {
    }

    public TestdataBothAnnotatedChildEntity(String code) {
        super(code);
    }

    public TestdataValue getSubValue() {
        return subValue;
    }

    public void setSubValue(TestdataValue subValue) {
        this.subValue = subValue;
    }
}
