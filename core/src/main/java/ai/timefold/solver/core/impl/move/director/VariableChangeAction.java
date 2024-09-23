package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

record VariableChangeAction<Solution_, Entity_, Value_>(Entity_ entity, Value_ oldValue,
        VariableDescriptor<Solution_> variableDescriptor) implements ChangeAction<Solution_> {

    @Override
    public void undo(InnerScoreDirector<Solution_, ?> scoreDirector) {
        scoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, oldValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);
    }

}