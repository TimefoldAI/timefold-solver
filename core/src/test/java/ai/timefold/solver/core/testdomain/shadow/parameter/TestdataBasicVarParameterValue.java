package ai.timefold.solver.core.testdomain.shadow.parameter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;

@PlanningEntity
public class TestdataBasicVarParameterValue {
    public static final LocalDateTime DEFAULT_TIME =
            LocalDateTime.of(2025, 4, 29, 18, 40, 0);

    String id;

    @ShadowVariable(supplierName = "calculateStartTime")
    LocalDateTime startTime;

    @ShadowVariable(supplierName = "calculateEndTime")
    LocalDateTime endTime;

    @ShadowVariablesInconsistent
    boolean isInvalid;

    @InverseRelationShadowVariable(sourceVariableName = "value")
    List<TestdataBasicVarParameterEntity> entityList = new ArrayList<>();

    Duration duration;

    public TestdataBasicVarParameterValue() {
    }

    public TestdataBasicVarParameterValue(String id, Duration duration) {
        this.id = id;
        this.duration = duration;
    }

    public List<TestdataBasicVarParameterEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataBasicVarParameterEntity> entityList) {
        this.entityList = entityList;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @ShadowSources({ "entityList" })
    public LocalDateTime calculateStartTime(TestdataBasicVarParameterSolution solution) {
        if (solution == null) {
            throw new IllegalStateException("Solution is null");
        }
        LocalDateTime readyTime = DEFAULT_TIME.plusDays(10);
        if (!entityList.isEmpty()) {
            readyTime = DEFAULT_TIME.plusDays(entityList.size());
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
    public LocalDateTime calculateEndTime(TestdataBasicVarParameterSolution solution) {
        if (solution == null) {
            throw new IllegalStateException("Solution is null");
        }
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
