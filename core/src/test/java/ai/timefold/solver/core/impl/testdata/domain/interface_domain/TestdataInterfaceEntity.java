package ai.timefold.solver.core.impl.testdata.domain.interface_domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public interface TestdataInterfaceEntity {
    @PlanningId
    String getId();

    @PlanningVariable
    TestdataInterfaceValue getValue();

    void setValue(TestdataInterfaceValue value);
}
