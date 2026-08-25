package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class UpdateShadowVariablesAction<Solution_> implements ChangeAction<Solution_> {

    private static final UpdateShadowVariablesAction<?> INSTANCE = new UpdateShadowVariablesAction<>();

    /**
     * The action is stateless ({@code Solution_} only occurs in a parameter position),
     * so the single shared instance serves every solution type.
     */
    @SuppressWarnings("unchecked")
    public static <Solution_> UpdateShadowVariablesAction<Solution_> instance() {
        return (UpdateShadowVariablesAction<Solution_>) INSTANCE;
    }

    private UpdateShadowVariablesAction() {
    }

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.updateShadowVariables();
    }

    @Override
    public UpdateShadowVariablesAction<Solution_> rebase(Lookup lookup) {
        return this;
    }
}
