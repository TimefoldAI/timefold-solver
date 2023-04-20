package ai.timefold.solver.quarkus.testdata.gizmo;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

public class PrivateNoArgsConstructorConstraintProvider implements ConstraintProvider {

    private PrivateNoArgsConstructorConstraintProvider() {
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEachUniquePair(PrivateNoArgsConstructorEntity.class,
                        Joiners.equal(p -> p.value))
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Same value")
        };
    }
}
