package ai.timefold.solver.core.impl.testdata.domain.declarative.task_assignment;

import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataTAConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataTATask.class)
                        .filter(TestdataTATask::isInvalid)
                        .penalize(HardSoftScore.ONE_HARD)
                        .asConstraint("Invalid task"),
                constraintFactory.forEach(TestdataTATask.class)
                        .filter(task -> !task.isInvalid())
                        .penalize(HardSoftScore.ONE_SOFT, t -> (int) Duration.between(t.getEmployee().getStartTime(),
                                t.getEndTime()).toMinutes())
                        .asConstraint("Finish tasks as early as possible")
        };
    }
}