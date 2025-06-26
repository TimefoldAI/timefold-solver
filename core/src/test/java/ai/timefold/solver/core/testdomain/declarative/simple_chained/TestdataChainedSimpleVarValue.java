package ai.timefold.solver.core.testdomain.declarative.simple_chained;

import java.time.Duration;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

@PlanningEntity
public class TestdataChainedSimpleVarValue {
    String id;

    @InverseRelationShadowVariable(sourceVariableName = "previous")
    TestdataChainedSimpleVarEntity next;

    Duration duration;

    @ShadowVariable(supplierName = "updateCumulativeDurationInDays")
    int cumulativeDurationInDays;

    public TestdataChainedSimpleVarValue() {
    }

    public TestdataChainedSimpleVarValue(String id, Duration duration) {
        this.id = id;
        this.duration = duration;
        this.cumulativeDurationInDays = (int) duration.toDays();
    }

    public TestdataChainedSimpleVarEntity getNext() {
        return next;
    }

    public void setNext(TestdataChainedSimpleVarEntity next) {
        this.next = next;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getCumulativeDurationInDays() {
        return cumulativeDurationInDays;
    }

    @ShadowSources("next.cumulativeDurationInDays")
    public int updateCumulativeDurationInDays() {
        if (next == null) {
            return (int) duration.toDays();
        } else {
            return next.getCumulativeDurationInDays() + (int) duration.toDays();
        }
    }
}
