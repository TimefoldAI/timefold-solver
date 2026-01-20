package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NullMarked;

/**
 * Dummy constraint provider that creates a single dummy constraint.
 * This is needed to create a valid score director factory without actual constraint evaluation.
 * <p>
 * This class is in the impl package (not public API) but must be public for reflection-based instantiation.
 */
@NullMarked
public final class DummyConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                constraintFactory.forEach(Object.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("Dummy constraint")
        };
    }
}
