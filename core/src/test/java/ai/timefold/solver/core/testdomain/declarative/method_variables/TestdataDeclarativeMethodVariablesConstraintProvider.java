package ai.timefold.solver.core.testdomain.declarative.method_variables;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

public class TestdataDeclarativeMethodVariablesConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(TestdataDeclarativeMethodVariablesSubclassValue.class)
                        .filter(value -> value.getCodeSum() != null)
                        .expand(value -> value.getCodeSum().chars().distinct().count())
                        .filter((value, distinctCount) -> distinctCount.intValue() != value.getCodeSum().length())
                        .penalize(HardSoftScore.ONE_HARD,
                                (value, distinctCount) -> value.getCodeSum().length() - distinctCount.intValue())
                        .asConstraint("Duplicate characters in code sum"),
                constraintFactory.forEach(TestdataDeclarativeMethodVariablesSubclassValue.class)
                        .filter(value -> value.getCodeSum() != null)
                        .reward(HardSoftScore.ONE_SOFT, value -> value.getCodeSum().length())
                        .asConstraint("Maximize code sum length")
        };
    }
}
