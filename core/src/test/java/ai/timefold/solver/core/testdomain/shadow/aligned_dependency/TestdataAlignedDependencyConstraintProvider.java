package ai.timefold.solver.core.testdomain.shadow.aligned_dependency;

import java.time.Duration;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataAlignedDependencyConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                finishValuesAsSoonAsPossible(constraintFactory),
        };
    }

    public Constraint finishValuesAsSoonAsPossible(@NonNull ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataAlignedDependencyValue.class)
                // A value has no end time while one of its dependencies or aligned values is unassigned.
                .filter(value -> value.getEndTime() != null)
                .penalize(HardSoftScore.ONE_SOFT, value -> (int) Duration.between(value.getEntity().getStartTime(),
                        value.getEndTime()).toMinutes())
                .asConstraint("Finish values as soon as possible");
    }
}
