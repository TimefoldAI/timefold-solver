package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class TriggerVariableListenersAction<Solution_> implements ChangeAction<Solution_> {

    private static final TriggerVariableListenersAction<?> INSTANCE = new TriggerVariableListenersAction<>();

    /**
     * The action is stateless ({@code Solution_} only occurs in a parameter position),
     * so the single shared instance serves every solution type.
     */
    @SuppressWarnings("unchecked")
    public static <Solution_> TriggerVariableListenersAction<Solution_> instance() {
        return (TriggerVariableListenersAction<Solution_>) INSTANCE;
    }

    private TriggerVariableListenersAction() {
    }

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.triggerVariableListeners();
    }

    @Override
    public TriggerVariableListenersAction<Solution_> rebase(Lookup lookup) {
        return this;
    }
}
