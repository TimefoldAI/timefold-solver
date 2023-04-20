package ai.timefold.solver.spring.boot.autoconfigure.gizmo.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.spring.boot.autoconfigure.gizmo.domain.TestdataGizmoSpringEntity;

public class TestdataGizmoConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataGizmoSpringEntity.class)
                        .join(TestdataGizmoSpringEntity.class, Joiners.equal(TestdataGizmoSpringEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same value.")
        };
    }

}
