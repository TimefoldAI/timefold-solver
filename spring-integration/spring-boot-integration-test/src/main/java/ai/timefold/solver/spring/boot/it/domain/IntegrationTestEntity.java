package ai.timefold.solver.spring.boot.it.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class IntegrationTestEntity {
    @PlanningId
    private String id;

    @PlanningVariable
    private IntegrationTestValue value;

    public IntegrationTestEntity() {
    }

    public IntegrationTestEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IntegrationTestValue getValue() {
        return value;
    }

    public void setValue(IntegrationTestValue value) {
        this.value = value;
    }
}
