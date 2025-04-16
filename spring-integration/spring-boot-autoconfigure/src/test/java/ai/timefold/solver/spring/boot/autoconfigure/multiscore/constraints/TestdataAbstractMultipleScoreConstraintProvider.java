package ai.timefold.solver.spring.boot.autoconfigure.multiscore.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.spring.boot.autoconfigure.multiscore.domain.TestdataMultipleScoreEntity;

import org.jspecify.annotations.NonNull;

public abstract class TestdataAbstractMultipleScoreConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataMultipleScoreEntity.class)
                        .join(TestdataMultipleScoreEntity.class, Joiners.equal(TestdataMultipleScoreEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same value.")
        };
    }
}
