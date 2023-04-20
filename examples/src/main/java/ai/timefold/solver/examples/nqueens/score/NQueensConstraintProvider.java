package ai.timefold.solver.examples.nqueens.score;

import static ai.timefold.solver.core.api.score.stream.Joiners.equal;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.examples.nqueens.domain.Queen;

public class NQueensConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                horizontalConflict(factory),
                ascendingDiagonalConflict(factory),
                descendingDiagonalConflict(factory),
        };
    }

    // ************************************************************************
    // Hard constraints
    // ************************************************************************

    protected Constraint horizontalConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Queen.class, equal(Queen::getRowIndex))
                .penalize(SimpleScore.ONE)
                .asConstraint("Horizontal conflict");
    }

    protected Constraint ascendingDiagonalConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Queen.class, equal(Queen::getAscendingDiagonalIndex))
                .penalize(SimpleScore.ONE)
                .asConstraint("Ascending diagonal conflict");
    }

    protected Constraint descendingDiagonalConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Queen.class, equal(Queen::getDescendingDiagonalIndex))
                .penalize(SimpleScore.ONE)
                .asConstraint("Descending diagonal conflict");
    }

}
