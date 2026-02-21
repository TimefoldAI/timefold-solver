package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

record VariableChangeAction<Solution_, Entity_, Value_>(Entity_ entity, Value_ oldValue,
        VariableDescriptor<Solution_> variableDescriptor) implements ChangeAction<Solution_> {

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, oldValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public ChangeAction<Solution_> rebase(Lookup lookup) {
        return new VariableChangeAction<>(lookup.lookUpWorkingObject(entity), lookup.lookUpWorkingObject(oldValue),
                variableDescriptor);
    }

}