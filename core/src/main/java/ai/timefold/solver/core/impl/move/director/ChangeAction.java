package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

sealed interface ChangeAction<Solution_>
        permits ListVariableAfterAssignmentAction, ListVariableAfterChangeAction, ListVariableAfterUnassignmentAction,
        ListVariableBeforeAssignmentAction, ListVariableBeforeChangeAction, ListVariableBeforeUnassignmentAction,
        VariableChangeAction {

    void undo(InnerScoreDirector<Solution_, ?> scoreDirector);

}