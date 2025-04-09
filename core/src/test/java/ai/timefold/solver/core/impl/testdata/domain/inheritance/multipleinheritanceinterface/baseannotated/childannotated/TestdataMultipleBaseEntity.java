package ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritanceinterface.baseannotated.childannotated;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotated.TestdataBaseEntity;

@PlanningEntity
public interface TestdataMultipleBaseEntity extends TestdataBaseEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange2")
    String getValue2();

    void setValue2(String value2);

}
