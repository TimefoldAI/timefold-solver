package ai.timefold.solver.quarkus.testdomain.gizmo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
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
}
