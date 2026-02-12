package ai.timefold.solver.core.testdomain.shadow.inverserelation;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataInverseRelationConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataInverseRelationValue.class)
                        .penalize(SimpleScore.ONE, value -> value.getEntities().size() * value.getEntities().size())
                        .asConstraint("Balance values"),
                constraintFactory.forEachIncludingUnassigned(TestdataInverseRelationEntity.class)
                        .filter(entity -> entity.getValue() == null)
                        .penalize(SimpleScore.of(100))
                        .asConstraint("Unassigned entity")
        };
    }
}
