package ai.timefold.solver.quarkus.testdata.invalid.inverserelation.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataInvalidInverseRelationEntity {

    private TestdataInvalidInverseRelationValue value;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataInvalidInverseRelationValue getValue() {
        return value;
    }

    public void setValue(TestdataInvalidInverseRelationValue value) {
        this.value = value;
    }

}
