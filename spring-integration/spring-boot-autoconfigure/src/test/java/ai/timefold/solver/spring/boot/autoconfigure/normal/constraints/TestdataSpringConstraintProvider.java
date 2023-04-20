package ai.timefold.solver.spring.boot.autoconfigure.normal.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringEntity;

public class TestdataSpringConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataSpringEntity.class)
                        .join(TestdataSpringEntity.class, Joiners.equal(TestdataSpringEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same value.")
        };
    }

}
