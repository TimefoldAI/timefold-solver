package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class VariableChangeAction<Solution_, Entity_, Value_> implements ChangeAction<Solution_> {

    private final Entity_ entity;
    private final Value_ oldValue;
    private final VariableDescriptor<Solution_> variableDescriptor;

    VariableChangeAction(Entity_ entity, Value_ oldValue, VariableDescriptor<Solution_> variableDescriptor) {
        this.entity = entity;
        this.oldValue = oldValue;
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    public void undo(InnerScoreDirector<Solution_, ?> scoreDirector) {
        scoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, oldValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);
    }
}