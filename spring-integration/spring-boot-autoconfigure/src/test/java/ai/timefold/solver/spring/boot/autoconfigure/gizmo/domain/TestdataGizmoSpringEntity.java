package ai.timefold.solver.spring.boot.autoconfigure.gizmo.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataGizmoSpringEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public String value;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
