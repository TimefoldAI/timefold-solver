package ai.timefold.solver.spring.boot.autoconfigure.chained.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringAnchor;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringEntity;

public class TestdataChainedSpringConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataChainedSpringAnchor.class)
                        .ifNotExists(TestdataChainedSpringEntity.class,
                                Joiners.equal((anchor) -> anchor, TestdataChainedSpringEntity::getPrevious))
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Assign at least one entity to each anchor.")
        };
    }

}
