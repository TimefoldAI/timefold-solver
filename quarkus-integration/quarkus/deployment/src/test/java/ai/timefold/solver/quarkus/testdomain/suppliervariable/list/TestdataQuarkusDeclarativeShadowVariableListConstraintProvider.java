package ai.timefold.solver.quarkus.testdomain.suppliervariable.list;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ForEachInclude;

import org.jspecify.annotations.NonNull;

public class TestdataQuarkusDeclarativeShadowVariableListConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataQuarkusDeclarativeShadowVariableListValue.class)
                        .penalize(SimpleScore.ONE, TestdataQuarkusDeclarativeShadowVariableListValue::getStartTime)
                        .asConstraint("Minimize start time"),
                factory.forEachIncluding(TestdataQuarkusDeclarativeShadowVariableListValue.class, ForEachInclude.INCONSISTENT)
                        .filter(TestdataQuarkusDeclarativeShadowVariableListValue::isInconsistent)
                        .penalize(SimpleScore.of(1000))
                        .asConstraint("Inconsistent")
        };
    }

}
