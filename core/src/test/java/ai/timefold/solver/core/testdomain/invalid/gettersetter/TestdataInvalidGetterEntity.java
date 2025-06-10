package ai.timefold.solver.core.testdomain.invalid.gettersetter;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataInvalidGetterEntity {

    @PlanningVariable
    private TestdataValue valueWithoutSetter;

    public TestdataValue getValueWithoutSetter() {
        return valueWithoutSetter;
    }

}
