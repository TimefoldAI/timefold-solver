package ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.basenotannotated;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

public interface TestdataBaseEntity {

    @PlanningId
    Long getId();

    void setId(Long id);

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    String getValue();

    void setValue(String value);

}
