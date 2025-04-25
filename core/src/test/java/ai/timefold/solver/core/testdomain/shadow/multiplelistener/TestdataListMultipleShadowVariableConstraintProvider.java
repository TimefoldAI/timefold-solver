package ai.timefold.solver.core.testdomain.shadow.multiplelistener;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public final class TestdataListMultipleShadowVariableConstraintProvider implements ConstraintProvider {
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
        return constraintFactory.forEach(TestdataListMultipleShadowVariableValue.class)
                .penalize(SimpleScore.ONE, TestdataListMultipleShadowVariableValue::getCascadeValue)
                .asConstraint("Penalize by cascade values");
    }

    public Constraint rewardCascadingUpdate(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataListMultipleShadowVariableValue.class)
                .reward(SimpleScore.ONE, v -> v.getCascadeValue() * 2)
                .asConstraint("Reward by cascade values");
    }

    public Constraint penalizeListener(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataListMultipleShadowVariableValue.class)
                .penalize(SimpleScore.ONE, TestdataListMultipleShadowVariableValue::getListenerValue)
                .asConstraint("Penalize by listener values");
    }

    public Constraint rewardListener(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataListMultipleShadowVariableValue.class)
                .reward(SimpleScore.ONE, v -> v.getListenerValue() * 2)
                .asConstraint("Reward by listener values");
    }
}
