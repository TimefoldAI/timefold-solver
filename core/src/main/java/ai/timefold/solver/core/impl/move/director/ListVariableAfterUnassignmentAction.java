package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.api.move.SolutionState;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

record ListVariableAfterUnassignmentAction<Solution_>(Object element,
        ListVariableDescriptor<Solution_> variableDescriptor) implements ChangeAction<Solution_> {

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.beforeListVariableElementAssigned(variableDescriptor, element);
    }

    @Override
    public ChangeAction<Solution_> rebase(SolutionState<Solution_> solutionState) {
        return new ListVariableAfterUnassignmentAction<>(solutionState.rebase(element), variableDescriptor);
    }

}