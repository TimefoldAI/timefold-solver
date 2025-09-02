package ai.timefold.solver.quarkus.testdomain.declarative.list;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataQuarkusDeclarativeShadowVariableListConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataQuarkusDeclarativeShadowVariableListValue.class)
                        .penalize(SimpleScore.ONE, TestdataQuarkusDeclarativeShadowVariableListValue::getStartTime)
                        .asConstraint("Minimize start time"),
                factory.forEachUnfiltered(TestdataQuarkusDeclarativeShadowVariableListValue.class)
                        .filter(TestdataQuarkusDeclarativeShadowVariableListValue::isInconsistent)
                        .penalize(SimpleScore.of(1000))
                        .asConstraint("Inconsistent")
        };
    }

}
