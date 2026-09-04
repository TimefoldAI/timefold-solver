package ai.timefold.solver.quarkus.testdomain.failing;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;

import org.jspecify.annotations.NonNull;

/**
 * Throws while the score is being calculated, but only once a specific value has been assigned.
 * That keeps the failure inside the solving process itself, rather than at problem installation:
 * the entities start out unassigned, so a solve only fails if its value range contains {@link #FAILING_VALUE}.
 */
public class TestdataQuarkusFailingConstraintProvider implements ConstraintProvider {

    public static final String FAILING_VALUE = "fail";
    public static final String FAILURE_MESSAGE = "Deliberate failure while calculating the score.";

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[] {
                factory.forEach(TestdataQuarkusEntity.class)
                        .filter(entity -> {
                            if (FAILING_VALUE.equals(entity.getValue())) {
                                throw new IllegalStateException(FAILURE_MESSAGE);
                            }
                            return false;
                        })
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Deliberate failure")
        };
    }
}
