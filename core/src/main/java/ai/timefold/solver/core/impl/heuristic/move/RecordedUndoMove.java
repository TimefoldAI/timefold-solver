package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;

public final class RecordedUndoMove<Solution_> extends AbstractUndoMove<Solution_> {

    private final Runnable undo;

    RecordedUndoMove(AbstractSimplifiedMove<Solution_> simplifiedMove, Runnable undo) {
        super(simplifiedMove);
        this.undo = Objects.requireNonNull(undo);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        undo.run();
    }
}