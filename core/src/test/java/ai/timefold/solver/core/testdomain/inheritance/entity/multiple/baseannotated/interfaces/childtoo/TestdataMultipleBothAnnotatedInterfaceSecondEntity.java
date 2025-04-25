package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public interface TestdataMultipleBothAnnotatedInterfaceSecondEntity extends TestdataMultipleBothAnnotatedInterfaceBaseEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange2")
    String getValue2();

    void setValue2(String value2);

}
