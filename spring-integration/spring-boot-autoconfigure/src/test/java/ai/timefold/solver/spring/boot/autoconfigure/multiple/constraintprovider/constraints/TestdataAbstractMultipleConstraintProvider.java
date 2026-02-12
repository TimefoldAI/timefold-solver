package ai.timefold.solver.spring.boot.autoconfigure.multiple.constraintprovider.constraints;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.spring.boot.autoconfigure.multiple.constraintprovider.domain.TestdataMultipleConstraintEntity;

import org.jspecify.annotations.NonNull;

public abstract class TestdataAbstractMultipleConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataMultipleConstraintEntity.class)
                        .join(TestdataMultipleConstraintEntity.class, Joiners.equal(TestdataMultipleConstraintEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same value.")
        };
    }
}
