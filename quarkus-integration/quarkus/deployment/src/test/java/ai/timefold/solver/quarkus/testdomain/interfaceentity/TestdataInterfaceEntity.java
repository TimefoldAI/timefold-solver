package ai.timefold.solver.quarkus.testdomain.interfaceentity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public interface TestdataInterfaceEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    Integer getValue();

    void setValue(Integer value);
}
