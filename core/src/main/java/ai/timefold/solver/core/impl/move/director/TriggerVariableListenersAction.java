package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Rebaser;

public final class TriggerVariableListenersAction<Solution_> implements ChangeAction<Solution_> {
    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.triggerVariableListeners();
    }

    @Override
    public TriggerVariableListenersAction<Solution_> rebase(Rebaser rebaser) {
        return this;
    }
}
