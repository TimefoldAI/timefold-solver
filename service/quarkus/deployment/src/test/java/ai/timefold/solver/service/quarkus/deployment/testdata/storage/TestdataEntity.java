package ai.timefold.solver.service.quarkus.deployment.testdata.storage;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataEntity {

    private String value;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
