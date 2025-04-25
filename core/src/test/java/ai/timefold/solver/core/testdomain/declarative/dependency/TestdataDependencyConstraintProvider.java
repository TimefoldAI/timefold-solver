package ai.timefold.solver.core.testdomain.declarative.dependency;

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
                constraintFactory.forEach(TestdataDependencyValue.class)
                        .filter(TestdataDependencyValue::isInvalid)
                        .penalize(HardSoftScore.ONE_HARD)
                        .asConstraint("Invalid task"),
                constraintFactory.forEach(TestdataDependencyValue.class)
                        .filter(task -> !task.isInvalid())
                        .penalize(HardSoftScore.ONE_SOFT, t -> (int) Duration.between(t.getEntity().getStartTime(),
                                t.getEndTime()).toMinutes())
                        .asConstraint("Finish tasks as early as possible")
        };
    }
}