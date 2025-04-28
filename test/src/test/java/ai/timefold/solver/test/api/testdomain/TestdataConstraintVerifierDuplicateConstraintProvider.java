package ai.timefold.solver.test.api.testdomain;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public final class TestdataConstraintVerifierDuplicateConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                penalizeEveryEntity(constraintFactory),
                penalizeEveryEntity(constraintFactory)
        };
    }

    public Constraint penalizeEveryEntity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Penalize every standard entity");
    }

}
