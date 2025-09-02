package ai.timefold.solver.core.testdomain.declarative.follower;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataFollowerEntity extends TestdataObject implements TestdataHasValue {
    TestdataLeaderEntity leader;

    // TODO: Remove me when supplier present
    @ShadowVariablesInconsistent
    boolean isInconsistent;

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

    @ShadowSources(value = "leader.value", alignmentKey = "leader")
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
