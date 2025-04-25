package ai.timefold.solver.core.testdomain.constraintconfiguration;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;

import org.jspecify.annotations.NonNull;

@Deprecated(forRemoval = true, since = "1.13.0")
public final class TestdataConstraintWeightConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                onlyConstraint(constraintFactory)
        };
    }

    private Constraint onlyConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataEntity.class)
                .rewardConfigurable()
                .asConstraint("First weight");
    }

}
