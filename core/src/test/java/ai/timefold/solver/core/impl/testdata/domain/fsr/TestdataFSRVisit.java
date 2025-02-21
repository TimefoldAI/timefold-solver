package ai.timefold.solver.core.impl.testdata.domain.fsr;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.preview.api.variable.provided.InvalidityMarker;
import ai.timefold.solver.core.preview.api.variable.provided.ProvidedShadowVariable;

@PlanningEntity
public class TestdataFSRVisit {
    String id;

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataFSRVehicle vehicle;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceReadyTime;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceStartTime;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceFinishTime;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataFSRVisit previousVisit;

    @NextElementShadowVariable(sourceVariableName = "visits")
    TestdataFSRVisit nextVisit;

    List<TestdataFSRVisit> visitGroup;

    @InvalidityMarker
    boolean isInvalid;

    public TestdataFSRVisit() {
    }

    public TestdataFSRVisit(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataFSRVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataFSRVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataFSRVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataFSRVisit previousVisit) {
        this.previousVisit = previousVisit;
    }

    public TestdataFSRVisit getNextVisit() {
        return nextVisit;
    }

    public void setNextVisit(TestdataFSRVisit nextVisit) {
        this.nextVisit = nextVisit;
    }

    public LocalDateTime getServiceReadyTime() {
        return serviceReadyTime;
    }

    public void setServiceReadyTime(LocalDateTime serviceReadyTime) {
        this.serviceReadyTime = serviceReadyTime;
    }

    public LocalDateTime getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(LocalDateTime serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public LocalDateTime getServiceFinishTime() {
        return serviceFinishTime;
    }

    public void setServiceFinishTime(LocalDateTime serviceFinishTime) {
        this.serviceFinishTime = serviceFinishTime;
    }

    public List<TestdataFSRVisit> getVisitGroup() {
        return visitGroup;
    }

    public void setVisitGroup(List<TestdataFSRVisit> visitGroup) {
        this.visitGroup = visitGroup;
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

    boolean getExpectedInvalid(Map<TestdataFSRVisit, Boolean> cache) {
        if (cache.containsKey(this)) {
            return cache.get(this);
        }
        cache.put(this, true);
        if (previousVisit != null && previousVisit.getExpectedInvalid(cache)) {
            return true;
        }
        if (visitGroup != null) {
            var vehicles = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var visit : visitGroup) {
                if (visit.vehicle != null && !vehicles.add(visit.vehicle)) {
                    return true;
                }
                if (visit != this) {
                    if (visit.previousVisit != null && visit.previousVisit.getExpectedInvalid(cache)) {
                        return true;
                    }
                }
            }
        }
        cache.put(this, false);
        return false;
    }

    record TimeCache(Map<TestdataFSRVisit, LocalDateTime> readyTimeCache, Map<TestdataFSRVisit, LocalDateTime> startTimeCache,
            Map<TestdataFSRVisit, LocalDateTime> endTimeCache) {
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
        if (previousVisit == null) {
            if (vehicle == null) {
                return null;
            }
            cache.readyTimeCache.put(this, TestShadowVariableProvider.BASE_START_TIME);
            return TestShadowVariableProvider.BASE_START_TIME;
        }
        var out = previousVisit.getExpectedServiceFinishTime(cache);
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
        if (visitGroup == null) {
            var out = getExpectedServiceReadyTime(cache);
            cache.startTimeCache.put(this, out);
            return out;
        } else {
            var out = visitGroup.stream().map(visit -> visit.getExpectedServiceReadyTime(cache))
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
        return vehicle != null;
    }

    @Override
    public String toString() {
        return (previousVisit != null) ? previousVisit + " -> " + id
                : (vehicle != null) ? vehicle.id + " -> " + id : "null -> " + id;
    }
}
