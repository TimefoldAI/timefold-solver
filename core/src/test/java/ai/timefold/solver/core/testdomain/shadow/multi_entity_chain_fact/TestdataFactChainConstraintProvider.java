package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fact;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NonNull;

/**
 * Penalizes by the shadow variables, so {@code FULL_ASSERT} catches them if they go stale.
 */
public class TestdataFactChainConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEachIncludingUnassigned(TestdataFactChainVisit.class)
                        .filter(visit -> visit.getVehicle() == null)
                        .penalize(SimpleScore.of(100))
                        .asConstraint("Assign all visits"),

                constraintFactory.forEach(TestdataFactChainVehicle.class)
                        .penalize(SimpleScore.ONE, TestdataFactChainVehicle::getEndTime)
                        .asConstraint("Minimize end time")
        };
    }
}
