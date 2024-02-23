package ai.timefold.solver.examples.nqueens.domain;

import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

public record CustomHorizontalConflictJustification(long queenId, long otherQueenId) implements ConstraintJustification {
}
