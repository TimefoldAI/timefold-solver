package ai.timefold.solver.core.testdomain.multivar;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataMultiVarConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEachIncludingUnassigned(TestdataMultiVarEntity.class)
                        .penalize(SimpleScore.ONE, entity -> {
                            int count = entity.getPrimaryValue() == entity.getSecondaryValue() ? 0 : 1;
                            count += entity.getTertiaryValueAllowedUnassigned() == null ? 0 : 1;
                            return count;
                        })
                        .asConstraint("testConstraint")
        };
    }

}
