package ai.timefold.solver.core.preview.api.variable.provided;

import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class RouteConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(Visit.class)
                        .filter(Visit::isInvalid)
                        .penalize(HardSoftScore.ONE_HARD)
                        .asConstraint("Invalid visit"),

                constraintFactory.forEach(Visit.class)
                        .filter(visit -> !visit.isInvalid() && visit.isAssigned())
                        .penalize(HardSoftScore.ONE_SOFT, visit -> (int) Duration
                                .between(TestShadowVariableProvider.BASE_START_TIME, visit.getServiceFinishTime()).toMinutes())
                        .asConstraint("Minimize finish time")
        };
    }
}
