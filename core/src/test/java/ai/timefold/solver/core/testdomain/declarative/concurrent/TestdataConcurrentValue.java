package ai.timefold.solver.core.testdomain.declarative.concurrent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableLooped;

@PlanningEntity
public class TestdataConcurrentValue {
    public static final LocalDateTime BASE_START_TIME = LocalDate.of(2025, 1, 1).atTime(LocalTime.of(9, 0));
    String id;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataConcurrentEntity entity;

    @ShadowVariable(supplierName = "serviceReadyTimeUpdater")
    LocalDateTime serviceReadyTime;

    @ShadowVariable(supplierName = "serviceStartTimeUpdater")
    LocalDateTime serviceStartTime;

    @ShadowVariable(supplierName = "serviceFinishTimeUpdater")
    LocalDateTime serviceFinishTime;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataConcurrentValue previousValue;

    @NextElementShadowVariable(sourceVariableName = "values")
    TestdataConcurrentValue nextValue;

    @IndexShadowVariable(sourceVariableName = "values")
    Integer index;

    @CascadingUpdateShadowVariable(targetMethodName = "cascadingMethod")
    LocalDateTime cascadingTime;

    List<TestdataConcurrentValue> concurrentValueGroup;

    @ShadowVariableLooped
    boolean isInvalid;

    public TestdataConcurrentValue() {
    }

    public TestdataConcurrentValue(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataConcurrentEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataConcurrentEntity entity) {
        this.entity = entity;
    }

    public TestdataConcurrentValue getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(TestdataConcurrentValue previousValue) {
        this.previousValue = previousValue;
    }

    public TestdataConcurrentValue getNextValue() {
        return nextValue;
    }

    public void setNextValue(TestdataConcurrentValue nextValue) {
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

    public LocalDateTime getCascadingTime() {
        return cascadingTime;
    }

    public void setCascadingTime(LocalDateTime cascadingTime) {
        this.cascadingTime = cascadingTime;
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

    @ShadowSources({ "serviceReadyTime", "concurrentValueGroup[].serviceReadyTime" })
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

    public void cascadingMethod() {
        if (index == null) {
            cascadingTime = null;
        } else {
            cascadingTime = BASE_START_TIME.plusDays(index);
        }
    }

    public LocalDateTime getServiceFinishTime() {
        return serviceFinishTime;
    }

    public void setServiceFinishTime(LocalDateTime serviceFinishTime) {
        this.serviceFinishTime = serviceFinishTime;
    }

    public List<TestdataConcurrentValue> getConcurrentValueGroup() {
        return concurrentValueGroup;
    }

    public void setConcurrentValueGroup(List<TestdataConcurrentValue> concurrentValueGroup) {
        this.concurrentValueGroup = concurrentValueGroup;
    }

    public boolean isInvalid() {
        return isInvalid;
    }

    public void setInvalid(boolean invalid) {
        isInvalid = invalid;
    }

    public boolean getExpectedInvalid() {
        return getExpectedInvalid(new IdentityHashMap<>());
    }

    boolean getExpectedInvalid(Map<TestdataConcurrentValue, Boolean> cache) {
        if (cache.containsKey(this)) {
            return cache.get(this);
        }
        cache.put(this, true);
        if (previousValue != null && previousValue.getExpectedInvalid(cache)) {
            return true;
        }
        if (concurrentValueGroup != null) {
            var vehicles = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var visit : concurrentValueGroup) {
                if (visit.entity != null && !vehicles.add(visit.entity)) {
                    return true;
                }
                if (visit != this && visit.previousValue != null && visit.previousValue.getExpectedInvalid(cache)) {
                    return true;
                }
            }
        }
        cache.put(this, false);
        return false;
    }

    record TimeCache(Map<TestdataConcurrentValue, LocalDateTime> readyTimeCache,
            Map<TestdataConcurrentValue, LocalDateTime> startTimeCache,
            Map<TestdataConcurrentValue, LocalDateTime> endTimeCache) {
        static TimeCache create() {
            return new TimeCache(new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>());
        }
    }

    public LocalDateTime getExpectedServiceReadyTime() {
        return getExpectedServiceReadyTime(TimeCache.create());
    }

    public LocalDateTime getExpectedServiceReadyTime(TimeCache cache) {
        if (getExpectedInvalid()) {
            return null;
        }
        if (cache.readyTimeCache.containsKey(this)) {
            return cache.readyTimeCache.get(this);
        }
        cache.readyTimeCache.put(this, null);
        if (previousValue == null) {
            if (entity == null) {
                return null;
            }
            cache.readyTimeCache.put(this, BASE_START_TIME);
            return BASE_START_TIME;
        }
        var out = previousValue.getExpectedServiceFinishTime(cache);
        if (out == null) {
            return null;
        }
        out = out.plusMinutes(30L);
        cache.readyTimeCache.put(this, out);
        return out;
    }

    public LocalDateTime getExpectedServiceStartTime() {
        return getExpectedServiceStartTime(TimeCache.create());
    }

    public LocalDateTime getExpectedServiceStartTime(TimeCache cache) {
        if (getExpectedInvalid()) {
            return null;
        }
        if (cache.startTimeCache.containsKey(this)) {
            return cache.startTimeCache.get(this);
        }
        cache.startTimeCache.put(this, null);
        if (concurrentValueGroup == null) {
            var out = getExpectedServiceReadyTime(cache);
            cache.startTimeCache.put(this, out);
            return out;
        } else {
            var out = concurrentValueGroup.stream().map(visit -> visit.getExpectedServiceReadyTime(cache))
                    .filter(Objects::nonNull).max(LocalDateTime::compareTo)
                    .orElse(null);
            cache.startTimeCache.put(this, out);
            return out;
        }
    }

    public LocalDateTime getExpectedServiceFinishTime() {
        return getExpectedServiceFinishTime(TimeCache.create());
    }

    public LocalDateTime getExpectedServiceFinishTime(TimeCache cache) {
        if (getExpectedInvalid()) {
            return null;
        }
        if (cache.endTimeCache.containsKey(this)) {
            return cache.endTimeCache.get(this);
        }
        cache.endTimeCache.put(this, null);
        var out = getExpectedServiceStartTime(cache);
        if (out == null) {
            return null;
        }
        out = out.plusMinutes(30L);
        cache.endTimeCache.put(this, out);
        return out;
    }

    public boolean isAssigned() {
        return entity != null;
    }

    @Override
    public String toString() {
        return (previousValue != null) ? previousValue + " -> " + id
                : (entity != null) ? entity.id + " -> " + id : "null -> " + id;
    }
}
