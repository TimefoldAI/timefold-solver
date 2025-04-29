package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.mixed;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestMultipleMixedConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataMultipleMixedChildEntity.class)
                        .filter(e -> e.getValue() != null)
                        .reward(SimpleScore.ONE, value -> 1)
                        .asConstraint("Constraint"),
                factory.forEach(TestdataMultipleMixedChildEntity.class)
                        .filter(e -> e.getValue2() != null)
                        .reward(SimpleScore.ONE, value -> 1)
                        .asConstraint("Constraint 2"),
                factory.forEach(TestdataMultipleMixedChildEntity.class)
                        .filter(e -> e.getValue3() != null)
                        .reward(SimpleScore.ONE, value -> 1)
                        .asConstraint("Constraint 3")
        };
    }

}
