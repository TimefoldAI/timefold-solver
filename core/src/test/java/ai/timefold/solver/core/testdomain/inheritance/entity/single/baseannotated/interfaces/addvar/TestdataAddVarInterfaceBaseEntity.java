package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.addvar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public interface TestdataAddVarInterfaceBaseEntity {

    @PlanningId
    Long getId();

    void setId(Long id);

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    String getValue();

    void setValue(String value);

}
