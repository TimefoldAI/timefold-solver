package ai.timefold.solver.model.testmodel.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.model.definition.api.ModelInput;
import ai.timefold.solver.model.definition.api.ModelOutput;
import ai.timefold.solver.model.definition.api.SolverModel;
import ai.timefold.solver.model.definition.api.metrics.InputMetricsAware;
import ai.timefold.solver.model.definition.api.metrics.OutputMetricsAware;
import ai.timefold.solver.model.testmodel.EmployeeScheduleInputMetrics;
import ai.timefold.solver.model.testmodel.EmployeeScheduleOutputMetrics;

import com.fasterxml.jackson.annotation.JsonIgnore;

@PlanningSolution
public class EmployeeSchedule implements ModelInput, ModelOutput, SolverModel<HardMediumSoftScore>,
        InputMetricsAware<EmployeeScheduleInputMetrics>, OutputMetricsAware<EmployeeScheduleOutputMetrics> {

    @ValueRangeProvider
    private List<Employee> employees;

    @PlanningEntityCollectionProperty
    private List<Shift> shifts;

    @PlanningScore
    private HardMediumSoftScore score;
    @JsonIgnore
    private ConstraintWeightOverrides<HardMediumSoftScore> constraintWeightOverrides;

    public EmployeeSchedule() {
        this.constraintWeightOverrides = ConstraintWeightOverrides.none();
    }

    public EmployeeSchedule(List<Employee> employees, List<Shift> shifts) {
        this.employees = employees;
        this.shifts = shifts;
        this.constraintWeightOverrides = ConstraintWeightOverrides.none();
    }

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }

    public List<Shift> getShifts() {
        return shifts;
    }

    public void setShifts(List<Shift> shifts) {
        this.shifts = shifts;
    }

    public HardMediumSoftScore getScore() {
        return score;
    }

    public void setScore(HardMediumSoftScore score) {
        this.score = score;
    }

    @JsonIgnore
    @Override
    public EmployeeScheduleInputMetrics getInputMetrics() {
        return new EmployeeScheduleInputMetrics(shifts.size(), employees.size());
    }

    @JsonIgnore
    @Override
    public EmployeeScheduleOutputMetrics getOutputMetrics() {
        int assignedShifts = (int) shifts.stream()
                .filter(shift -> shift.getEmployee() != null)
                .count();
        return new EmployeeScheduleOutputMetrics(assignedShifts);
    }

    @Override
    public ConstraintWeightOverrides<HardMediumSoftScore> getConstraintWeightOverrides() {
        return this.constraintWeightOverrides;
    }

    public void setConstraintWeightOverrides(ConstraintWeightOverrides<HardMediumSoftScore> constraintWeightOverrides) {
        this.constraintWeightOverrides = constraintWeightOverrides;
    }
}
