package ai.timefold.solver.core.testdomain.list.unassignedvar.pinned;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public final class TestdataPinnedUnassignedValuesListConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                entityConstraint(constraintFactory),
                valueConstraint(constraintFactory)
        };
    }

    private Constraint entityConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataPinnedUnassignedValuesListEntity.class)
                .penalize(SimpleScore.ONE, e -> e.getValueList().size())
                .asConstraint("Entity list size");
    }

    private Constraint valueConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachIncludingUnassigned(TestdataPinnedUnassignedValuesListValue.class)
                .filter(value -> value.getEntity() == null)
                .penalize(SimpleScore.ONE)
                .asConstraint("Unassigned values");
    }

}
