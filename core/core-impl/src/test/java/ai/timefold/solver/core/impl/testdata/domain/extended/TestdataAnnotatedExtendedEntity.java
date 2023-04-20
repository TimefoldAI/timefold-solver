package ai.timefold.solver.core.impl.testdata.domain.extended;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningEntity
public class TestdataAnnotatedExtendedEntity extends TestdataEntity {

    private TestdataValue subValue;

    public TestdataAnnotatedExtendedEntity() {
    }

    public TestdataAnnotatedExtendedEntity(String code) {
        super(code);
    }

    public TestdataAnnotatedExtendedEntity(String code, TestdataValue value) {
        super(code, value);
    }

    @PlanningVariable(valueRangeProviderRefs = "subValueRange")
    public TestdataValue getSubValue() {
        return subValue;
    }

    public void setSubValue(TestdataValue subValue) {
        this.subValue = subValue;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
