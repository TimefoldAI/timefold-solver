package ai.timefold.solver.core.preview.api.variable.declarative.task_assignment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment.TestdataTAConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment.TestdataTAEmployee;
import ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment.TestdataTASchedule;
import ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment.TestdataTATask;

import org.junit.jupiter.api.Test;

public class TAShadowVariableTest {
    @Test
    public void testSolve() {
        var baseTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        var employeeA = new TestdataTAEmployee(baseTime);
        var employeeB = new TestdataTAEmployee(baseTime.plusMinutes(30));
        var employeeC = new TestdataTAEmployee(baseTime.plusMinutes(60));
        var employeeD = new TestdataTAEmployee(baseTime.plusMinutes(90));

        var taskA = new TestdataTATask("A", Duration.ofMinutes(30));
        var taskB = new TestdataTATask("B", Duration.ofMinutes(10));
        var taskC = new TestdataTATask("C", Duration.ofMinutes(50));
        var taskD = new TestdataTATask("D", Duration.ofMinutes(120));

        var taskA2 = new TestdataTATask("(A,B)", Duration.ofMinutes(40), List.of(taskA, taskB));
        var taskB2 = new TestdataTATask("(B,C)", Duration.ofMinutes(10), List.of(taskB, taskC));
        var taskC2 = new TestdataTATask("(C,D)", Duration.ofMinutes(40), List.of(taskC, taskD));
        var taskD2 = new TestdataTATask("(A,D)", Duration.ofMinutes(40), List.of(taskA, taskD));

        var taskA3 = new TestdataTATask("(A,B)+C", Duration.ofMinutes(40), List.of(taskA2, taskC));
        var taskB3 = new TestdataTATask("(B,C)+D", Duration.ofMinutes(10), List.of(taskB2, taskD));
        var taskC3 = new TestdataTATask("(C,D)+A", Duration.ofMinutes(40), List.of(taskC2, taskA));
        var taskD3 = new TestdataTATask("(A,D)+B", Duration.ofMinutes(40), List.of(taskD2, taskB));

        var schedule = new TestdataTASchedule(List.of(employeeA, employeeB, employeeC, employeeD),
                List.of(taskA, taskB, taskC, taskD,
                        taskA2, taskB2, taskC2, taskD2,
                        taskA3, taskB3, taskC3, taskD3));

        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataTASchedule.class)
                .withEntityClasses(TestdataTAEmployee.class, TestdataTATask.class)
                .withConstraintProviderClass(TestdataTAConstraintProvider.class)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1_000L));

        var solverFactory = SolverFactory.<TestdataTASchedule> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(schedule);

        for (var task : solution.getTasks()) {
            if (task.getDependencies() != null) {
                for (var dependency : task.getDependencies()) {
                    assertThat(task.getStartTime()).isAfterOrEqualTo(dependency.getEndTime());
                }
            }
        }
    }
}
