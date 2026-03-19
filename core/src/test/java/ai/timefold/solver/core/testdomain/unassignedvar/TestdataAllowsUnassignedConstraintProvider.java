package ai.timefold.solver.core.testdomain.unassignedvar;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

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
                .join(constraintFactory.forEachIncludingUnassigned(TestdataAllowsUnassignedEntity.class),
                        Joiners.equal(TestdataAllowsUnassignedEntity::getValue))
                .penalize(SimpleScore.ONE)
                .asConstraint("testConstraint");
    }

}