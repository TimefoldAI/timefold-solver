package ai.timefold.solver.service.quarkus.deployment.it;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

import org.jspecify.annotations.NonNull;

public class TestdataConstraintProvider implements ConstraintProvider {

    public static final SimpleScore NO_CONFLICTS_CONSTRAINT_WEIGHT = SimpleScore.ONE;
    public static final String NO_CONFLICTS_CONSTRAINT_NAME = "noConflicts";

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataEntity.class)
                        .join(TestdataEntity.class, Joiners.equal(TestdataEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(NO_CONFLICTS_CONSTRAINT_WEIGHT)
                        .asConstraint(NO_CONFLICTS_CONSTRAINT_NAME)
        };
    }
}
