package ai.timefold.solver.quarkus.testdomain.gizmo;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class PrivateNoArgsConstructorEntity {
    @PlanningId
    final String id;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    String value;

    private PrivateNoArgsConstructorEntity() {
        id = null;
    }

    public PrivateNoArgsConstructorEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
