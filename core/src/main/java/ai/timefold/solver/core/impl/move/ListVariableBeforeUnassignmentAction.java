package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

record ListVariableBeforeUnassignmentAction<Solution_>(Object element,
        ListVariableDescriptor<Solution_> variableDescriptor) implements ChangeAction<Solution_> {

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.afterListVariableElementAssigned(variableDescriptor, element);
    }

    @Override
    public ChangeAction<Solution_> rebase(Lookup lookup) {
        return new ListVariableBeforeUnassignmentAction<>(lookup.lookUpWorkingObject(element), variableDescriptor);
    }

}