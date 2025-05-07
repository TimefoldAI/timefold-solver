package ai.timefold.solver.core.testdomain.declarative.chained;

import java.time.Duration;
import java.time.LocalDateTime;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableLooped;

@PlanningEntity
public class TestdataChainedVarValue {
    public static final LocalDateTime DEFAULT_TIME =
            LocalDateTime.of(2025, 4, 29, 18, 40, 0);

    String id;

    @ShadowVariable(supplierName = "calculateStartTime")
    LocalDateTime startTime;

    @ShadowVariable(supplierName = "calculateEndTime")
    LocalDateTime endTime;

    @ShadowVariableLooped
    boolean isInvalid;

    @InverseRelationShadowVariable(sourceVariableName = "previous")
    TestdataChainedVarEntity next;

    Duration duration;

    public TestdataChainedVarValue() {
    }

    public TestdataChainedVarValue(String id, Duration duration) {
        this.id = id;
        this.duration = duration;
    }

    public TestdataChainedVarEntity getNext() {
        return next;
    }

    public void setNext(TestdataChainedVarEntity next) {
        this.next = next;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @ShadowSources({ "next" })
    public LocalDateTime calculateStartTime() {
        LocalDateTime readyTime = null;
        if (next != null) {
            readyTime = DEFAULT_TIME.plusDays(1);
        }
        return readyTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @ShadowSources({ "startTime" })
    public LocalDateTime calculateEndTime() {
        if (startTime == null) {
            return null;
        }
        return startTime.plus(duration);
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public boolean isInvalid() {
        return isInvalid;
    }

    public void setInvalid(boolean invalid) {
        isInvalid = invalid;
    }

    @Override
    public String toString() {
        return id + "{" +
                "endTime=" + endTime +
                "]}";
    }
}
