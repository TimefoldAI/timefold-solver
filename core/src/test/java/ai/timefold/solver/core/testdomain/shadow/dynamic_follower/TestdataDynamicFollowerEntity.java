package ai.timefold.solver.core.testdomain.shadow.dynamic_follower;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataDynamicFollowerEntity extends TestdataObject implements TestdataDynamicHasValue {
    @PlanningVariable
    TestdataDynamicLeaderEntity leader;

    @ShadowVariable(supplierName = "valueSupplier")
    TestdataValue value;

    public TestdataDynamicFollowerEntity() {
    }

    public TestdataDynamicFollowerEntity(String code) {
        super(code);
    }

    public TestdataDynamicFollowerEntity(String code, TestdataDynamicLeaderEntity leader) {
        super(code);
        this.leader = leader;
    }

    @Override
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    @ShadowSources(value = "leader.value")
    public TestdataValue valueSupplier() {
        if (leader == null) {
            return null;
        }
        return leader.value;
    }

    public TestdataDynamicLeaderEntity getLeader() {
        return leader;
    }

    public void setLeader(TestdataDynamicLeaderEntity leader) {
        this.leader = leader;
    }
}
