package ai.timefold.solver.test.api.score.stream.testdata.shadow.list;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public final class TestdataListShadowConstraintVerifierConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                penalizeCascadingUpdate(constraintFactory),
                rewardCascadingUpdate(constraintFactory),
                penalizeListener(constraintFactory),
                rewardListener(constraintFactory),

        };
    }

    public Constraint penalizeCascadingUpdate(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataListShadowConstraintVerifierValue.class)
                .penalize(SimpleScore.ONE, TestdataListShadowConstraintVerifierValue::getCascadeValue)
                .asConstraint("Penalize by cascade values");
    }

    public Constraint rewardCascadingUpdate(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataListShadowConstraintVerifierValue.class)
                .reward(SimpleScore.ONE, v -> v.getCascadeValue() * 2)
                .asConstraint("Reward by cascade values");
    }

    public Constraint penalizeListener(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataListShadowConstraintVerifierValue.class)
                .penalize(SimpleScore.ONE, TestdataListShadowConstraintVerifierValue::getListenerValue)
                .asConstraint("Penalize by listener values");
    }

    public Constraint rewardListener(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataListShadowConstraintVerifierValue.class)
                .reward(SimpleScore.ONE, v -> v.getListenerValue() * 2)
                .asConstraint("Reward by listener values");
    }
}
