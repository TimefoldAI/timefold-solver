package ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.multiple.baseannotated.interfaces.childtoo;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestMultipleBothAnnotatedInterfaceConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataMultipleBothAnnotatedInterfaceChildEntity.class)
                        .filter(e -> e.getValue() != null)
                        .reward(SimpleScore.ONE, value -> 1)
                        .asConstraint("Constraint"),
                factory.forEach(TestdataMultipleBothAnnotatedInterfaceChildEntity.class)
                        .filter(e -> e.getValue2() != null)
                        .reward(SimpleScore.ONE, value -> 1)
                        .asConstraint("Constraint 2")
        };
    }

}
