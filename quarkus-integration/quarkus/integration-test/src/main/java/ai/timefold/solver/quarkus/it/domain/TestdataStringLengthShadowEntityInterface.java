package ai.timefold.solver.quarkus.it.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public interface TestdataStringLengthShadowEntityInterface {
    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    String getValue();

    void setValue(String value);
}
