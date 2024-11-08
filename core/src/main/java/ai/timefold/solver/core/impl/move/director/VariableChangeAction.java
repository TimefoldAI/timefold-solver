package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Rebaser;

record VariableChangeAction<Solution_, Entity_, Value_>(Entity_ entity, Value_ oldValue,
        VariableDescriptor<Solution_> variableDescriptor) implements ChangeAction<Solution_> {

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, oldValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public ChangeAction<Solution_> rebase(Rebaser rebaser) {
        return new VariableChangeAction<>(rebaser.rebase(entity), rebaser.rebase(oldValue), variableDescriptor);
    }

}