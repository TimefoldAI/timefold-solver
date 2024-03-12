package ai.timefold.solver.core.impl.testdata.domain.planningid;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataStringPlanningIdEntity {
    @PlanningId
    private String planningId;

    @PlanningVariable(valueRangeProviderRefs = "stringValueRange")
    private String value;

    public TestdataStringPlanningIdEntity(String planningId) {
        this(planningId, null);
    }

    public TestdataStringPlanningIdEntity(String planningId, String value) {
        this.planningId = planningId;
        this.value = value;
    }

    public String getPlanningId() {
        return planningId;
    }

    public void setPlanningId(String planningId) {
        this.planningId = planningId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
