package ai.timefold.solver.spring.boot.autoconfigure.normal.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataSpringEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

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
