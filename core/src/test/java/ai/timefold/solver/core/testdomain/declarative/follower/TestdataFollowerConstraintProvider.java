package ai.timefold.solver.core.testdomain.declarative.follower;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataFollowerConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataHasValue.class)
                        .groupBy(TestdataHasValue::getValue, ConstraintCollectors.count())
                        .penalize(SimpleScore.ONE, (value, count) -> count * count)
                        .asConstraint("Minimize value count")
        };
    }
}
