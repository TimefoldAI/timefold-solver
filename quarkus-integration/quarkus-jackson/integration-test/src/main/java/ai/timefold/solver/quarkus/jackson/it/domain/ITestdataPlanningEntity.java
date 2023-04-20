package ai.timefold.solver.quarkus.jackson.it.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class ITestdataPlanningEntity {

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
