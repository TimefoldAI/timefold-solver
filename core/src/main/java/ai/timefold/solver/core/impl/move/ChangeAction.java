package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public sealed interface ChangeAction<Solution_>
        permits ListVariableAfterAssignmentAction, ListVariableAfterChangeAction, ListVariableAfterUnassignmentAction,
        ListVariableBeforeAssignmentAction, ListVariableBeforeChangeAction, ListVariableBeforeUnassignmentAction,
        TriggerVariableListenersAction, VariableChangeAction {

    void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector);

    ChangeAction<Solution_> rebase(Lookup lookup);

}