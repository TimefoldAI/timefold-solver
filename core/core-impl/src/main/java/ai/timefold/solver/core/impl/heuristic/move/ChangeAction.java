package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

sealed interface ChangeAction<Solution_>
        permits ListVariableAfterAssignmentAction, ListVariableAfterChangeAction, ListVariableAfterUnassignmentAction,
        ListVariableBeforeAssignmentAction, ListVariableBeforeChangeAction, ListVariableBeforeUnassignmentAction,
        VariableChangeAction {

    void undo(InnerScoreDirector<Solution_, ?> scoreDirector);

}