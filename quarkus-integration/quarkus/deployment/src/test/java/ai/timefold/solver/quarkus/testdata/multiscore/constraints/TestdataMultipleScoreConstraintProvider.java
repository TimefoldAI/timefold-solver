package ai.timefold.solver.quarkus.testdata.multiscore.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.quarkus.testdata.multiscore.domain.TestdataMultipleScoreEntity;

import org.jspecify.annotations.NonNull;

public class TestdataMultipleScoreConstraintProvider extends TestdataAbstractMultipleScoreConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataMultipleScoreEntity.class)
                        .join(TestdataMultipleScoreEntity.class, Joiners.equal(TestdataMultipleScoreEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(SimpleScore.ONE, (e1, e2) -> 1)
                        .asConstraint("Don't assign 2 entities the same value.")
        };
    }
}
