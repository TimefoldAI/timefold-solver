package ai.timefold.solver.core.testdomain.declarative.follower;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataFollowerEntity extends TestdataObject implements TestdataHasValue {
    TestdataLeaderEntity leader;

    @ShadowVariable(supplierName = "valueSupplier")
    TestdataValue value;

    public TestdataFollowerEntity() {
    }

    public TestdataFollowerEntity(String code, TestdataLeaderEntity leader) {
        super(code);
        this.leader = leader;
    }

    @Override
    public TestdataValue getValue() {
        return value;
    }

    @ShadowSources("leader.value")
    public TestdataValue valueSupplier() {
        return leader.value;
    }

    public TestdataLeaderEntity getLeader() {
        return leader;
    }

    public void setLeader(TestdataLeaderEntity leader) {
        this.leader = leader;
    }
}
