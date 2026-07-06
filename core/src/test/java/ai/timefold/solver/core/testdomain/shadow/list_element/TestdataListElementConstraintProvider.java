package ai.timefold.solver.core.testdomain.shadow.list_element;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

/**
 * Penalizes by the {@link TestdataListElementEntity#getLastEndTime() aggregate shadow variable},
 * so {@code FULL_ASSERT} catches it if it goes stale.
 */
public class TestdataListElementConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEachUnfiltered(TestdataListElementValue.class)
                        .filter(value -> value.getEntity() == null)
                        .penalize(SimpleScore.of(100))
                        .asConstraint("Assign all values"),

                constraintFactory.forEachUnfiltered(TestdataListElementEntity.class)
                        .filter(entity -> entity.getLastEndTime() != null)
                        .penalize(SimpleScore.ONE, TestdataListElementEntity::getLastEndTime)
                        .asConstraint("Minimize last end time")
        };
    }
}
