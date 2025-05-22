package ai.timefold.solver.quarkus.testdomain.suppliervariable.list;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataQuarkusSupplierVariableListConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataQuarkusSupplierVariableListValue.class)
                        .filter(value -> !value.isLooped())
                        .penalize(SimpleScore.ONE, TestdataQuarkusSupplierVariableListValue::getStartTime)
                        .asConstraint("Minimize start time"),
                factory.forEach(TestdataQuarkusSupplierVariableListValue.class)
                        .filter(TestdataQuarkusSupplierVariableListValue::isLooped)
                        .penalize(SimpleScore.of(1000))
                        .asConstraint("Looped")
        };
    }

}
