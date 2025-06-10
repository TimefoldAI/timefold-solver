package ai.timefold.solver.core.testdomain.invalid.gettersetter;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataDifferentGetterSetterVisibilityEntity {

    private TestdataValue value1;
    private TestdataValue value2;

    @PlanningVariable
    public TestdataValue getValue1() {
        return value1;
    }

    private void setValue1(TestdataValue value1) {
        this.value1 = value1;
    }

    @PlanningVariable
    TestdataValue getValue2() {
        return value2;
    }

    @PlanningVariable
    protected TestdataValue setValue2(TestdataValue value2) {
        return value2;
    }

}
