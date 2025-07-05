package ai.timefold.solver.core.testdomain.declarative.multi_directional_parent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableLooped;

@PlanningEntity
public class TestdataMultiDirectionConcurrentValue {
    public static final LocalDateTime BASE_START_TIME = LocalDate.of(2025, 1, 1).atTime(LocalTime.of(9, 0));
    String id;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataMultiDirectionConcurrentEntity entity;

    @ShadowVariable(supplierName = "serviceReadyTimeUpdater")
    LocalDateTime serviceReadyTime;

    @ShadowVariable(supplierName = "serviceStartTimeUpdater")
    LocalDateTime serviceStartTime;

    @ShadowVariable(supplierName = "serviceFinishTimeUpdater")
    LocalDateTime serviceFinishTime;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataMultiDirectionConcurrentValue previousValue;

    @NextElementShadowVariable(sourceVariableName = "values")
    TestdataMultiDirectionConcurrentValue nextValue;

    @IndexShadowVariable(sourceVariableName = "values")
    Integer index;

    List<TestdataMultiDirectionConcurrentValue> concurrentValueGroup;

    @ShadowVariableLooped
    boolean isInvalid;

    public TestdataMultiDirectionConcurrentValue() {
    }

    public TestdataMultiDirectionConcurrentValue(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataMultiDirectionConcurrentEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataMultiDirectionConcurrentEntity entity) {
        this.entity = entity;
    }

    public TestdataMultiDirectionConcurrentValue getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(TestdataMultiDirectionConcurrentValue previousValue) {
        this.previousValue = previousValue;
    }

    public TestdataMultiDirectionConcurrentValue getNextValue() {
        return nextValue;
    }

    public void setNextValue(TestdataMultiDirectionConcurrentValue nextValue) {
        this.nextValue = nextValue;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public LocalDateTime getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(LocalDateTime serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    @ShadowSources({ "previousValue.serviceFinishTime", "entity" })
    public LocalDateTime serviceReadyTimeUpdater() {
        if (previousValue != null) {
            return previousValue.serviceFinishTime.plusMinutes(30L);
        }
        if (entity != null) {
            return BASE_START_TIME;
        }
        return null;
    }

    @ShadowSources({ "serviceReadyTime", "concurrentValueGroup[].nextValue" })
    public LocalDateTime serviceStartTimeUpdater() {
        if (serviceReadyTime == null) {
            return null;
        }
        var startTime = serviceReadyTime;
        if (concurrentValueGroup != null) {
            for (var visit : concurrentValueGroup) {
                if (visit.serviceReadyTime != null && startTime.isBefore(visit.serviceReadyTime)) {
                    startTime = visit.serviceReadyTime;
                }
            }
        }
        return startTime;
    }

    @ShadowSources("serviceStartTime")
    public LocalDateTime serviceFinishTimeUpdater() {
        if (serviceStartTime == null) {
            return null;
        }
        return serviceStartTime.plusMinutes(30L);
    }

    public LocalDateTime getServiceFinishTime() {
        return serviceFinishTime;
    }

    public void setServiceFinishTime(LocalDateTime serviceFinishTime) {
        this.serviceFinishTime = serviceFinishTime;
    }

    public List<TestdataMultiDirectionConcurrentValue> getConcurrentValueGroup() {
        return concurrentValueGroup;
    }

    public void setConcurrentValueGroup(List<TestdataMultiDirectionConcurrentValue> concurrentValueGroup) {
        this.concurrentValueGroup = concurrentValueGroup;
    }

    public boolean isInvalid() {
        return isInvalid;
    }

    public void setInvalid(boolean invalid) {
        isInvalid = invalid;
    }

    @Override
    public String toString() {
        return (previousValue != null) ? previousValue + " -> " + id
                : (entity != null) ? entity.id + " -> " + id : "null -> " + id;
    }
}
