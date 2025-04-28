package ai.timefold.solver.quarkus.testdomain.suppliervariable.constraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.quarkus.testdomain.suppliervariable.domain.TestdataQuarkusSupplierVariableEntity;

import org.jspecify.annotations.NonNull;

public class TestdataQuarkusSupplierVariableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataQuarkusSupplierVariableEntity.class)
                        .join(TestdataQuarkusSupplierVariableEntity.class,
                                Joiners.equal(TestdataQuarkusSupplierVariableEntity::getValue1,
                                        TestdataQuarkusSupplierVariableEntity::getValue2))
                        .filter((a, b) -> {
                            if (a.getValue1AndValue2() == null || b.getValue1AndValue2() == null) {
                                throw new IllegalStateException();
                            }
                            return a.getValue1AndValue2().equals(b.getValue1AndValue2());
                        })
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Don't assign 2 entities the same value.")
        };
    }

}
