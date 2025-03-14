package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Rebaser;

public sealed interface ChangeAction<Solution_>
        permits ListVariableAfterAssignmentAction, ListVariableAfterChangeAction, ListVariableAfterUnassignmentAction,
        ListVariableBeforeAssignmentAction, ListVariableBeforeChangeAction, ListVariableBeforeUnassignmentAction,
        TriggerVariableListenersAction, VariableChangeAction {

    void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector);

    ChangeAction<Solution_> rebase(Rebaser rebaser);

}