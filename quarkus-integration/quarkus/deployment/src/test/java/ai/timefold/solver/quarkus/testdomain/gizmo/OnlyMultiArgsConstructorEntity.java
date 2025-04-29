package ai.timefold.solver.quarkus.testdomain.gizmo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class OnlyMultiArgsConstructorEntity extends PrivateNoArgsConstructorEntity {
    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    String anotherValue;

    public OnlyMultiArgsConstructorEntity(String id) {
        super(id);
    }
}
