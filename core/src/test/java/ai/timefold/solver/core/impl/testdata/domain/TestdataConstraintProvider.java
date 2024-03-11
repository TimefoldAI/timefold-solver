package ai.timefold.solver.core.impl.testdata.domain;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

public final class TestdataConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] { alwaysPenalizingConstraint(constraintFactory) };
    }

    private Constraint alwaysPenalizingConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataEntity.class)
                .penalize(SimpleScore.ONE)
                .asConstraint("Always penalize");
    }

}
