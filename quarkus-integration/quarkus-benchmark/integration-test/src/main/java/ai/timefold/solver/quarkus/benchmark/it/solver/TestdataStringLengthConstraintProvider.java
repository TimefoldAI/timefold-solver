package ai.timefold.solver.quarkus.benchmark.it.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.quarkus.benchmark.it.domain.TestdataListValueShadowEntity;
import ai.timefold.solver.quarkus.benchmark.it.domain.TestdataStringLengthShadowEntity;

import org.jspecify.annotations.NonNull;

public class TestdataStringLengthConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEachUniquePair(TestdataStringLengthShadowEntity.class)
                        .filter((a, b) -> a.getValues().stream().anyMatch(v -> b.getValues().contains(v)))
                        .penalize(HardSoftScore.ONE_HARD)
                        .asConstraint("Don't assign 2 entities the same value."),
                factory.forEach(TestdataListValueShadowEntity.class)
                        .reward(HardSoftScore.ONE_SOFT, a -> a.getLength())
                        .asConstraint("Maximize value length")
        };
    }

}
