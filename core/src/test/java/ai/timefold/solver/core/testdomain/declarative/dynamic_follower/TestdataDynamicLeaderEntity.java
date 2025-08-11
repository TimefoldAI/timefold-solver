package ai.timefold.solver.core.testdomain.declarative.dynamic_follower;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataDynamicLeaderEntity extends TestdataObject implements TestdataDynamicHasValue {
    @PlanningVariable
    TestdataValue value;

    public TestdataDynamicLeaderEntity() {
    }

    public TestdataDynamicLeaderEntity(String code) {
        super(code);
    }

    @Override
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }
}
