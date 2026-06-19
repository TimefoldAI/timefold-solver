package ai.timefold.solver.service.testmodel.domain;

import java.time.OffsetDateTime;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class Shift {

    private String id;

    private Skill requiredSkill;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    @PlanningVariable(allowsUnassigned = true)
    private Employee employee;

    public Shift() {
    }

    public Shift(String id, Skill requiredSkill) {
        this.id = id;
        this.requiredSkill = requiredSkill;
    }

    public Shift withStartAndEndTime(OffsetDateTime startTime, OffsetDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        return this;
    }

    public String getId() {
        return id;
    }

    public Skill getRequiredSkill() {
        return requiredSkill;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }
}
