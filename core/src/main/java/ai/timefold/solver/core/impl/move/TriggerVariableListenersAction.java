package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class TriggerVariableListenersAction<Solution_> implements ChangeAction<Solution_> {
    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.triggerVariableListeners();
    }

    @Override
    public TriggerVariableListenersAction<Solution_> rebase(Lookup lookup) {
        return this;
    }
}
