package ai.timefold.solver.core.testdomain.declarative.follower_set;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataHasValue;
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataLeaderEntity;

@PlanningEntity
public class TestdataFollowerSetEntity extends TestdataObject implements TestdataHasValue {
    List<TestdataLeaderEntity> leaders;

    @ShadowVariable(supplierName = "valueSupplier")
    TestdataValue value;

    public TestdataFollowerSetEntity() {
    }

    public TestdataFollowerSetEntity(String code, List<TestdataLeaderEntity> leaders) {
        super(code);
        this.leaders = leaders;
    }

    @Override
    public TestdataValue getValue() {
        return value;
    }

    @ShadowSources("leaders[].value")
    public TestdataValue valueSupplier() {
        var min = leaders.get(0).getValue();
        for (int i = 1; i < leaders.size(); i++) {
            var leader = leaders.get(i);
            var leaderValue = leader.getValue();
            if (min == null || (leaderValue != null && leaderValue.getCode().compareTo(min.getCode()) < 0)) {
                min = leaderValue;
            }
        }
        return min;
    }
}
