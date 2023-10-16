package ai.timefold.solver.quarkus.testdata.extended;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.extendedshadow.TestdataExtendedShadowEntity;

public class TestdataExtendedShadowSolutionConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataExtendedShadowEntity.class)
                        .filter(e -> e.myPlanningVariable.id != e.desiredId)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Variable does not match desired id")
        };
    }
}
