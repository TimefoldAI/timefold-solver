package ai.timefold.solver.core.testdomain.list.pinned;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public final class TestdataPinnedListConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                onlyConstraint(constraintFactory)
        };
    }

    private Constraint onlyConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataPinnedListEntity.class)
                .penalize(SimpleScore.ONE)
                .asConstraint("First weight");
    }

}
