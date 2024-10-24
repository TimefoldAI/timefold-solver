package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.api.move.Rebaser;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

sealed interface ChangeAction<Solution_>
        permits ListVariableAfterAssignmentAction, ListVariableAfterChangeAction, ListVariableAfterUnassignmentAction,
        ListVariableBeforeAssignmentAction, ListVariableBeforeChangeAction, ListVariableBeforeUnassignmentAction,
        VariableChangeAction {

    void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector);

    ChangeAction<Solution_> rebase(Rebaser rebaser);

}