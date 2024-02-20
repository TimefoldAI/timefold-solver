package ai.timefold.solver.test.api.score.stream.testdata;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.test.api.score.stream.testdata.justification.TestFirstComparableJustification;
import ai.timefold.solver.test.api.score.stream.testdata.justification.TestFirstJustification;

public final class TestdataConstraintVerifierConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                penalizeEveryEntity(constraintFactory),
                rewardEveryEntity(constraintFactory),
                impactEveryEntity(constraintFactory),
                differentStringEntityHaveDifferentValues(constraintFactory),
                justifyWithFirstJustification(constraintFactory),
                justifyWithFirstComparableJustification(constraintFactory)
        };
    }

    public Constraint penalizeEveryEntity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Penalize every standard entity");
    }

    public Constraint rewardEveryEntity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .reward(HardSoftScore.ofSoft(2))
                .asConstraint("Reward every standard entity");
    }

    public Constraint impactEveryEntity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .impact(HardSoftScore.ofHard(4),
                        entity -> Objects.equals(entity.getCode(), "A") ? 1 : -1)
                .asConstraint("Impact every standard entity");
    }

    public Constraint differentStringEntityHaveDifferentValues(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(TestdataConstraintVerifierSecondEntity.class,
                Joiners.equal(TestdataConstraintVerifierSecondEntity::getValue))
                .penalize(HardSoftScore.ofSoft(3))
                .asConstraint("Different String Entity Have Different Values");
    }

    public Constraint justifyWithFirstJustification(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .penalize(HardSoftScore.ONE_HARD)
                .justifyWith((entity, score) -> new TestFirstJustification(1))
                .asConstraint("Justify with first justification");
    }

    public Constraint justifyWithFirstComparableJustification(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .penalize(HardSoftScore.ONE_HARD)
                .justifyWith((entity, score) -> new TestFirstComparableJustification(1))
                .asConstraint("Justify with first comparable justification");
    }

}
