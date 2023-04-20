package ai.timefold.solver.quarkus.testdata.invalid.inverserelation.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.quarkus.testdata.invalid.inverserelation.domain.TestdataInvalidInverseRelationValue;

public class TestdataInvalidQuarkusConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataInvalidInverseRelationValue.class)
                        .filter(room -> room.getEntityList().size() > 1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same room.")
        };
    }

}
