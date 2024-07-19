package ai.timefold.solver.core.impl.testdata.domain.constraintweightoverrides;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;

public final class TestdataConstraintWeightOverridesConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                firstConstraint(constraintFactory),
                secondConstraint(constraintFactory)
        };
    }

    public Constraint firstConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataEntity.class)
                .penalize(SimpleScore.ONE)
                .asConstraint("First weight");
    }

    public Constraint secondConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataEntity.class)
                .reward(SimpleScore.of(2))
                .asConstraint("Second weight");
    }

}
