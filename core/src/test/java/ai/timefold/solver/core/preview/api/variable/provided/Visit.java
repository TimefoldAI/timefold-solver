package ai.timefold.solver.core.preview.api.variable.provided;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;

@PlanningEntity
public class Visit {
    String id;

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    Vehicle vehicle;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceReadyTime;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceStartTime;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceFinishTime;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    Visit previousVisit;

    @NextElementShadowVariable(sourceVariableName = "visits")
    Visit nextVisit;

    List<Visit> visitGroup;

    @InvalidityMarker
    boolean isInvalid;

    public Visit() {
    }

    public Visit(String id) {
        this.id = id;
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

    public List<Visit> getVisitGroup() {
        return visitGroup;
    }

    public void setVisitGroup(List<Visit> visitGroup) {
        this.visitGroup = visitGroup;
    }

    public boolean isInvalid() {
        return isInvalid;
    }

    public void setInvalid(boolean invalid) {
        isInvalid = invalid;
    }

    public boolean getExpectedInvalid() {
        if (previousVisit != null && previousVisit.getExpectedInvalid()) {
            return true;
        }
        if (visitGroup == null) {
            return false;
        }
        for (var visit : visitGroup) {
            if (visit.nextVisit != null && visit.nextVisit.getExpectedInvalid(visitGroup)) {
                return true;
            }
        }
        return false;
    }

    public boolean getExpectedInvalid(List<Visit> toCheck) {
        if (visitGroup != null && !Collections.disjoint(visitGroup, toCheck)) {
            return true;
        }
        if (nextVisit != null) {
            return nextVisit.getExpectedInvalid(toCheck);
        }
        return false;
    }

    public LocalDateTime getExpectedServiceReadyTime() {
        if (getExpectedInvalid()) {
            return null;
        }
        if (previousVisit == null) {
            if (vehicle == null) {
                return null;
            }
            return TestShadowVariableProvider.BASE_START_TIME;
        }
        return previousVisit.getExpectedServiceFinishTime().plusMinutes(30L);
    }

    public LocalDateTime getExpectedServiceStartTime() {
        if (getExpectedInvalid()) {
            return null;
        }
        if (visitGroup == null) {
            return getExpectedServiceReadyTime();
        } else {
            return visitGroup.stream().map(Visit::getExpectedServiceReadyTime)
                    .filter(Objects::nonNull).min(LocalDateTime::compareTo)
                    .orElse(null);
        }
    }

    public LocalDateTime getExpectedServiceFinishTime() {
        if (getExpectedInvalid()) {
            return null;
        }
        return getExpectedServiceStartTime().plusMinutes(30L);
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
