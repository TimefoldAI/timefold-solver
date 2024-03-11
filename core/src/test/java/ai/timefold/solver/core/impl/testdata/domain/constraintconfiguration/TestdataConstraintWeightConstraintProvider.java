package ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;

public final class TestdataConstraintWeightConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                onlyConstraint(constraintFactory)
        };
    }

    private Constraint onlyConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataEntity.class)
                .rewardConfigurable()
                .asConstraint("First weight");
    }

}
