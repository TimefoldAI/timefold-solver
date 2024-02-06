package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

public final class TestdataAllowsUnassignedValuesListConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                onlyConstraint(constraintFactory)
        };
    }

    private Constraint onlyConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataAllowsUnassignedValuesListEntity.class)
                .penalize(SimpleScore.ONE, e -> e.getValueList().size())
                .asConstraint("First weight");
    }

}
