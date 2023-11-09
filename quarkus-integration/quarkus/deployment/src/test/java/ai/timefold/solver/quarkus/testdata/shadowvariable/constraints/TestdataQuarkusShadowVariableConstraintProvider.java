package ai.timefold.solver.quarkus.testdata.shadowvariable.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.quarkus.testdata.shadowvariable.domain.TestdataQuarkusShadowVariableEntity;

public class TestdataQuarkusShadowVariableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataQuarkusShadowVariableEntity.class)
                        .join(TestdataQuarkusShadowVariableEntity.class,
                                Joiners.equal(TestdataQuarkusShadowVariableEntity::getValue1,
                                        TestdataQuarkusShadowVariableEntity::getValue2))
                        .filter((a, b) -> a.getValue1AndValue2().equals(b.getValue1AndValue2()))
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same value.")
        };
    }

}
