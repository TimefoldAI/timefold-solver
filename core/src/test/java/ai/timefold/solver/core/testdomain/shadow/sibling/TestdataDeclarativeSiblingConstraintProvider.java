package ai.timefold.solver.core.testdomain.shadow.sibling;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataDeclarativeSiblingConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataDeclarativeSiblingFirstValue.class)
                        .reward(SimpleScore.ONE, value -> value.getPreviousCode().length())
                        .asConstraint("First previous code length"),
                constraintFactory.forEach(TestdataDeclarativeSiblingSecondValue.class)
                        .reward(SimpleScore.ONE, value -> value.getPreviousCode().length())
                        .asConstraint("Second previous code length")
        };
    }
}
