package ai.timefold.solver.quarkus.testdomain.invalid.inverserelation;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataInvalidQuarkusConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataInvalidInverseRelationValue.class)
                        .filter(room -> room.getEntityList().size() > 1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same room.")
        };
    }

}
