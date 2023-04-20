package ai.timefold.solver.quarkus.it.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.quarkus.it.domain.TestdataStringLengthShadowEntity;

public class TestdataStringLengthConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataStringLengthShadowEntity.class)
                        .join(TestdataStringLengthShadowEntity.class, Joiners.equal(TestdataStringLengthShadowEntity::getValue))
                        .filter((a, b) -> a != b)
                        .penalize(HardSoftScore.ONE_HARD)
                        .asConstraint("Don't assign 2 entities the same value."),
                factory.forEach(TestdataStringLengthShadowEntity.class)
                        .reward(HardSoftScore.ONE_SOFT, TestdataStringLengthShadowEntity::getLength)
                        .asConstraint("Maximize value length")
        };
    }

}
