package ai.timefold.solver.quarkus.it.reflection.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataReflectionEntity {

    @PlanningVariable(valueRangeProviderRefs = "fieldValueRange")
    public String fieldValue;

    private String methodValueField;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @PlanningVariable(valueRangeProviderRefs = "methodValueRange")
    public String getMethodValue() {
        return methodValueField;
    }

    public void setMethodValue(String methodValueField) {
        this.methodValueField = methodValueField;
    }

}
