package ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment;

import java.util.EnumSet;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataTASchedule {
    @PlanningEntityCollectionProperty
    List<TestdataTAEmployee> employees;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataTATask> tasks;

    @PlanningScore
    HardSoftScore score;

    public TestdataTASchedule() {
    }

    public TestdataTASchedule(List<TestdataTAEmployee> employees, List<TestdataTATask> tasks) {
        this.tasks = tasks;
        this.employees = employees;
    }

    public SolutionDescriptor<TestdataTASchedule> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                TestdataTASchedule.class, TestdataTAEmployee.class, TestdataTATask.class);
    }

    public List<TestdataTATask> getTasks() {
        return tasks;
    }

    public void setTasks(List<TestdataTATask> tasks) {
        this.tasks = tasks;
    }

    public List<TestdataTAEmployee> getEmployees() {
        return employees;
    }

    public void setEmployees(
            List<TestdataTAEmployee> employees) {
        this.employees = employees;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "TestdataTASchedule{" +
                "employees=" + employees +
                ", tasks=" + tasks +
                ", score=" + score +
                '}';
    }
}
