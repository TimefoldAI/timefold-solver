package ai.timefold.solver.core.impl.testdata.domain.declarative.fsr;

import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataFSRConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataFSRVisit.class)
                        .filter(TestdataFSRVisit::isInvalid)
                        .penalize(HardSoftScore.ONE_HARD)
                        .asConstraint("Invalid visit"),

                constraintFactory.forEach(TestdataFSRVisit.class)
                        .filter(visit -> !visit.isInvalid() && visit.isAssigned())
                        .penalize(HardSoftScore.ONE_SOFT, visit -> (int) Duration
                                .between(TestdataFSRShadowVariableProvider.BASE_START_TIME, visit.getServiceFinishTime())
                                .toMinutes())
                        .asConstraint("Minimize finish time")
        };
    }
}
