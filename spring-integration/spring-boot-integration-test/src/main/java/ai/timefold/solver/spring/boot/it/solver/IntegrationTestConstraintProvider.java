package ai.timefold.solver.spring.boot.it.solver;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.spring.boot.it.domain.IntegrationTestEntity;

public class IntegrationTestConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(IntegrationTestEntity.class)
                        .filter(entity -> !entity.getId().equals(entity.getValue().id()))
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Entity id do not match value id")
        };
    }
}
