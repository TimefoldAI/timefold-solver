package ai.timefold.solver.core.impl.score.director;

import java.util.List;

public interface RevertableScoreDirector<Solution_> extends VariableDescriptorAwareScoreDirector<Solution_> {

    /**
     * Use this method to get a copy of all non-commited changes executed by the director so far.
     * 
     * @param <Action_> The action type for recorded changes
     */
    <Action_> List<Action_> copyChanges();

    /**
     * Use this method to revert all changes made by moves.
     * The score director that implements this logic must be able to track every single change in the solution and
     * restore it to its original state.
     */
    void undoChanges();
}
