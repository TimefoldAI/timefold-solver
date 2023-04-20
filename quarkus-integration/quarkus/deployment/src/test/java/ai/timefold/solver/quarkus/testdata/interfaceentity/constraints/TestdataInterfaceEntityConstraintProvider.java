package ai.timefold.solver.quarkus.testdata.interfaceentity.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.quarkus.testdata.interfaceentity.domain.TestdataInterfaceEntity;

public class TestdataInterfaceEntityConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataInterfaceEntity.class)
                        .penalize(SimpleScore.ONE, TestdataInterfaceEntity::getValue)
                        .asConstraint("Minimize value")
        };
    }
}
