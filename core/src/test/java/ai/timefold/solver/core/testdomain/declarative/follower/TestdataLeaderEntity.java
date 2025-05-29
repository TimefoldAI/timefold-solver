package ai.timefold.solver.core.testdomain.declarative.follower;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataLeaderEntity extends TestdataObject implements TestdataHasValue {
    @PlanningVariable
    TestdataValue value;

    public TestdataLeaderEntity() {
    }

    public TestdataLeaderEntity(String code) {
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
