package ai.timefold.solver.examples.taskassigning.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.examples.common.domain.AbstractPersistable;

@PlanningSolution
public class TaskAssigningSolution extends AbstractPersistable {

    @ProblemFactCollectionProperty
    private List<Skill> skillList;
    @ProblemFactCollectionProperty
    private List<TaskType> taskTypeList;
    @ProblemFactCollectionProperty
    private List<Customer> customerList;
    @ValueRangeProvider
    @ProblemFactCollectionProperty
    private List<Task> taskList;

    @PlanningEntityCollectionProperty
    private List<Employee> employeeList;

    @PlanningScore(bendableHardLevelsSize = 1, bendableSoftLevelsSize = 4)
    private BendableScore score;

    /** Relates to {@link Task#getStartTime()}. */
    private int frozenCutoff; // In minutes

    public TaskAssigningSolution() {
    }

    public TaskAssigningSolution(long id) {
        super(id);
    }

    public TaskAssigningSolution(long id, List<Skill> skillList, List<TaskType> taskTypeList,
            List<Customer> customerList, List<Employee> employeeList, List<Task> taskList) {
        this(id);
        this.skillList = skillList;
        this.taskTypeList = taskTypeList;
        this.customerList = customerList;
        this.employeeList = employeeList;
        this.taskList = taskList;
    }

    public List<Skill> getSkillList() {
        return skillList;
    }

    public void setSkillList(List<Skill> skillList) {
        this.skillList = skillList;
    }

    public List<TaskType> getTaskTypeList() {
        return taskTypeList;
    }

    public void setTaskTypeList(List<TaskType> taskTypeList) {
        this.taskTypeList = taskTypeList;
    }

    public List<Customer> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<Customer> customerList) {
        this.customerList = customerList;
    }

    public List<Employee> getEmployeeList() {
        return employeeList;
    }

    public void setEmployeeList(List<Employee> employeeList) {
        this.employeeList = employeeList;
    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }

    public BendableScore getScore() {
        return score;
    }

    public void setScore(BendableScore score) {
        this.score = score;
    }

    public int getFrozenCutoff() {
        return frozenCutoff;
    }

    public void setFrozenCutoff(int frozenCutoff) {
        this.frozenCutoff = frozenCutoff;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
