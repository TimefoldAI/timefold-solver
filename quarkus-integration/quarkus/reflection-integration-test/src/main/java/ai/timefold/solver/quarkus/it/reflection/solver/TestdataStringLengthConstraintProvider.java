package ai.timefold.solver.quarkus.it.reflection.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.quarkus.it.reflection.domain.TestdataReflectionEntity;

public class TestdataStringLengthConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataReflectionEntity.class)
                        .join(TestdataReflectionEntity.class, Joiners.equal(TestdataReflectionEntity::getMethodValue))
                        .filter((a, b) -> !a.fieldValue.equals(b.fieldValue))
                        .penalize(HardSoftScore.ONE_HARD)
                        .asConstraint("Entities with equal method values should have equal field values"),
                factory.forEach(TestdataReflectionEntity.class)
                        .reward(HardSoftScore.ONE_SOFT, entity -> entity.getMethodValue().length())
                        .asConstraint("Maximize method value length")
        };
    }

}
