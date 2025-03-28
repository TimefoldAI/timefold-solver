package ai.timefold.solver.core.impl.heuristic.move;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.director.VariableChangeRecordingScoreDirector;

/**
 * Abstract superclass for {@link Move}, requiring implementation of undo moves.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Move
 */
public abstract class AbstractMove<Solution_> implements Move<Solution_> {

    @Override
    public final void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        var recordingScoreDirector =
                scoreDirector instanceof VariableChangeRecordingScoreDirector<Solution_, ?> variableChangeRecordingScoreDirector
                        ? variableChangeRecordingScoreDirector
                        : new VariableChangeRecordingScoreDirector<>(scoreDirector);
        doMoveOnGenuineVariables(recordingScoreDirector);
        scoreDirector.triggerVariableListeners();
    }

    /**
     * Called before the move is done, so the move can be evaluated and then be undone
     * without resulting into a permanent change in the solution.
     *
     * @param scoreDirector the {@link ScoreDirector} not yet modified by the move.
     * @return an undoMove which does the exact opposite of this move.
     * @deprecated The solver automatically generates undo moves, this method is no longer used.
     */
    @Deprecated(forRemoval = true, since = "1.16.0")
    protected Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        throw new UnsupportedOperationException("Operation requires an undo move, which is no longer supported.");
    }

    /**
     * Like {@link #doMoveOnly(ScoreDirector)} but without the {@link ScoreDirector#triggerVariableListeners()} call
     * (because {@link #doMoveOnly(ScoreDirector)} already does that).
     *
     * @param scoreDirector never null
     */
    protected abstract void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector);

    // ************************************************************************
    // Util methods
    // ************************************************************************

    public static <E> List<E> rebaseList(List<E> externalObjectList, ScoreDirector<?> destinationScoreDirector) {
        var rebasedObjectList = new ArrayList<E>(externalObjectList.size());
        for (var entity : externalObjectList) {
            rebasedObjectList.add(destinationScoreDirector.lookUpWorkingObject(entity));
        }
        return rebasedObjectList;
    }

    public static <E> Set<E> rebaseSet(Set<E> externalObjectSet, ScoreDirector<?> destinationScoreDirector) {
        var rebasedObjectSet = new LinkedHashSet<E>(externalObjectSet.size());
        for (var entity : externalObjectSet) {
            rebasedObjectSet.add(destinationScoreDirector.lookUpWorkingObject(entity));
        }
        return rebasedObjectSet;
    }

}
