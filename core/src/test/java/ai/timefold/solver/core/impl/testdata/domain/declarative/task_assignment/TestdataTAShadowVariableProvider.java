package ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment;

import java.time.LocalDateTime;
import java.util.List;

import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableFactory;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableProvider;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class TestdataTAShadowVariableProvider implements ShadowVariableProvider {

    @Nullable
    private static LocalDateTime getMaxEndTime(TestdataTATask task, LocalDateTime initialStartTime,
            List<@Nullable LocalDateTime> dependencyEndTimes, String name) {
        var maxEndTime = initialStartTime;
        for (LocalDateTime endTime : dependencyEndTimes) {
            if (endTime == null) {
                return null;
            }
            if (endTime.isAfter(maxEndTime)) {
                maxEndTime = endTime;
            }
        }
        return maxEndTime;
    }

    @Override
    public void defineVariables(ShadowVariableFactory shadowVariableFactory) {
        var task = shadowVariableFactory.entity(TestdataTATask.class);
        var taskDependencies = task.group(TestdataTATask.class, TestdataTATask::getDependencies);
        var taskDependencyEndTimes = taskDependencies.variables(LocalDateTime.class, "endTime").allowNullValues();

        var previousTask = task.previous();
        var previousTaskEndTime = previousTask.variable(LocalDateTime.class, "endTime");

        var taskEmployee = task.inverse(TestdataTAEmployee.class);
        var taskEmployeeStartTime = taskEmployee.fact(LocalDateTime.class, TestdataTAEmployee::getStartTime);

        var startTime = shadowVariableFactory.newShadow(TestdataTATask.class)
                .computeIfHasAll(
                        taskDependencyEndTimes,
                        previousTaskEndTime,
                        (t, dependencyEndTimes, previousEndTime) -> getMaxEndTime(t, previousEndTime, dependencyEndTimes,
                                "previous"))
                .elseComputeIfHasAll(
                        taskDependencyEndTimes,
                        taskEmployeeStartTime,
                        (t, dependencyEndTimes, employeeStartTime) -> getMaxEndTime(t, employeeStartTime, dependencyEndTimes,
                                "no previous"))
                .elseComputeIfHasAll(previousTaskEndTime, (t, endTime) -> endTime)
                .elseComputeIfHasAll(taskEmployeeStartTime, (t, endTime) -> endTime)
                .as("startTime");

        var endTime = shadowVariableFactory.newShadow(TestdataTATask.class)
                .computeIfHasAll(startTime, (t, taskStartTime) -> taskStartTime.plus(t.getDuration()))
                .as("endTime");
    }
}
