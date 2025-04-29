package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childnot;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public interface TestdataChildNotAnnotatedInterfaceBaseEntity {

    @PlanningId
    Long getId();

    void setId(Long id);

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    String getValue();

    void setValue(String value);

}
