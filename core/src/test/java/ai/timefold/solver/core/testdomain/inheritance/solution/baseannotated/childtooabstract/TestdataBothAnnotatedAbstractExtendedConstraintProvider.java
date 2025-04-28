package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;

import org.jspecify.annotations.NonNull;

public class TestdataBothAnnotatedAbstractExtendedConstraintProvider extends TestdataBothAnnotatedAbstractConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                super.defineConstraints(factory)[0],
                factory.forEach(TestdataBothAnnotatedAbstractChildEntity.class)
                        .filter(e -> e.getValue() != null)
                        .reward(SimpleScore.ONE, value -> 1)
                        .asConstraint("Constraint2")
        };
    }

}
