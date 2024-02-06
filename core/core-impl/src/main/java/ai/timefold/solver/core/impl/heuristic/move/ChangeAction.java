package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

sealed interface ChangeAction<Solution_>
        permits ListVariableAfterAssignmentAction, ListVariableAfterChangeAction, ListVariableAfterInitializationAction,
        ListVariableAfterUnassignmentAction, ListVariableAfterUninitializationAction, ListVariableBeforeAssignmentAction,
        ListVariableBeforeChangeAction, ListVariableBeforeInitializationAction, ListVariableBeforeUnassignmentAction,
        ListVariableBeforeUninitializationAction, VariableChangeAction {

    void undo(InnerScoreDirector<Solution_, ?> scoreDirector);

}