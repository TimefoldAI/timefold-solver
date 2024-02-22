package ai.timefold.solver.core.impl.heuristic.move;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

/**
 * Abstract superclass for {@link Move}, requiring implementation of undo moves.
 * Unless raw performance is a concern, consider using {@link AbstractSimplifiedMove} instead.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Move
 */
public abstract class AbstractMove<Solution_> implements Move<Solution_> {

    @Override
    public final Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
        var undoMove = createUndoMove(scoreDirector);
        doMoveOnly(scoreDirector);
        return undoMove;
    }

    @Override
    public final void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        doMoveOnGenuineVariables(scoreDirector);
        scoreDirector.triggerVariableListeners();
    }

    /**
     * Called before the move is done, so the move can be evaluated and then be undone
     * without resulting into a permanent change in the solution.
     *
     * @param scoreDirector the {@link ScoreDirector} not yet modified by the move.
     * @return an undoMove which does the exact opposite of this move.
     */
    protected abstract Move<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector);

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
        List<E> rebasedObjectList = new ArrayList<>(externalObjectList.size());
        for (E entity : externalObjectList) {
            rebasedObjectList.add(destinationScoreDirector.lookUpWorkingObject(entity));
        }
        return rebasedObjectList;
    }

    public static Object[] rebaseArray(Object[] externalObjects, ScoreDirector<?> destinationScoreDirector) {
        Object[] rebasedObjects = new Object[externalObjects.length];
        for (int i = 0; i < externalObjects.length; i++) {
            rebasedObjects[i] = destinationScoreDirector.lookUpWorkingObject(externalObjects[i]);
        }
        return rebasedObjects;
    }

}
