package ai.timefold.solver.core.testdomain.declarative.concurrent;

import static ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentValue.BASE_START_TIME;

import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataConcurrentAssertionConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataConcurrentValue.class)
                        .filter(TestdataConcurrentValue::getExpectedInvalid)
                        .penalize(HardSoftScore.ONE_HARD)
                        .asConstraint("Invalid visit"),

                constraintFactory.forEach(TestdataConcurrentValue.class)
                        .filter(visit -> !visit.getExpectedInvalid() && visit.isAssigned())
                        .penalize(HardSoftScore.ONE_SOFT, visit -> (int) Duration
                                .between(BASE_START_TIME,
                                        visit.getExpectedServiceFinishTime())
                                .toMinutes())
                        .asConstraint("Minimize finish time")
        };
    }
}
