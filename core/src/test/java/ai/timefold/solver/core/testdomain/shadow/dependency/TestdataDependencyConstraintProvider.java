package ai.timefold.solver.core.testdomain.shadow.dependency;

import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataDependencyConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                penalizeInconsistentTasks(constraintFactory),
                finishTasksAsSoonAsPossible(constraintFactory),
        };
    }

    public Constraint penalizeInconsistentTasks(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUnfiltered(TestdataDependencyValue.class)
                .filter(TestdataDependencyValue::getIsInvalid)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Invalid task");
    }

    public Constraint finishTasksAsSoonAsPossible(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataDependencyValue.class)
                .penalize(HardSoftScore.ONE_SOFT, t -> (int) Duration.between(t.getEntity().getStartTime(),
                        t.getEndTime()).toMinutes())
                .asConstraint("Finish tasks as early as possible");
    }
}