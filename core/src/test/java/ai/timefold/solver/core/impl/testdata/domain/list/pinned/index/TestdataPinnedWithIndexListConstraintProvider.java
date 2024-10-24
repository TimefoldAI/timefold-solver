package ai.timefold.solver.core.impl.testdata.domain.list.pinned.index;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public final class TestdataPinnedWithIndexListConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                onlyConstraint(constraintFactory)
        };
    }

    private Constraint onlyConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataPinnedWithIndexListEntity.class)
                .penalize(SimpleScore.ONE)
                .asConstraint("First weight");
    }

}
