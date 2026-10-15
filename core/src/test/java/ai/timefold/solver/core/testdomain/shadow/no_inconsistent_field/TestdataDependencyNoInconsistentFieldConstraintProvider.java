package ai.timefold.solver.core.testdomain.shadow.no_inconsistent_field;

import java.time.Duration;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataDependencyNoInconsistentFieldConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                finishTasksAsSoonAsPossible(constraintFactory),
        };
    }

    public Constraint finishTasksAsSoonAsPossible(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataDependencyNoInconsistentFieldValue.class)
                .penalize(HardSoftScore.ONE_SOFT, t -> (int) Duration.between(t.getEntity().getStartTime(),
                        t.getEndTime()).toMinutes())
                .asConstraint("Finish tasks as early as possible");
    }
}