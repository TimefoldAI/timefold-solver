package ai.timefold.solver.core.preview.api.variable.provided;

import java.time.LocalDateTime;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class Visit {
    String id;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceReadyTime;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceStartTime;

    @ProvidedShadowVariable(TestShadowVariableProvider.class)
    LocalDateTime serviceFinishTime;

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

    @Override
    public String toString() {
        return id;
    }
}
