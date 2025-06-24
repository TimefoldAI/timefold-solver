package ai.timefold.solver.core.testdomain.unassignedvar;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public final class TestdataAllowsUnassignedConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                valueConstraint(constraintFactory)
        };
    }

    private Constraint valueConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachIncludingUnassigned(TestdataAllowsUnassignedEntity.class)
                .filter(entity -> entity.getValue() == null)
                .penalize(SimpleScore.ONE)
                .asConstraint("Unassigned entities");
    }

}
