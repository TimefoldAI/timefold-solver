package ai.timefold.solver.core.testdomain.declarative.dynamic_follower;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariablesInconsistent;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataDynamicFollowerEntity extends TestdataObject implements TestdataDynamicHasValue {
    @PlanningVariable
    TestdataDynamicLeaderEntity leader;

    // TODO: Remove me when supplier present
    @ShadowVariablesInconsistent
    boolean isInconsistent;

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
