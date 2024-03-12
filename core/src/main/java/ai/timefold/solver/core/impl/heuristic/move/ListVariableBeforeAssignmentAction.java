package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ListVariableBeforeAssignmentAction<Solution_> implements ChangeAction<Solution_> {

    private final Object element;
    private final ListVariableDescriptor<Solution_> variableDescriptor;

    ListVariableBeforeAssignmentAction(Object element, ListVariableDescriptor<Solution_> variableDescriptor) {
        this.element = element;
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    public void undo(InnerScoreDirector<Solution_, ?> scoreDirector) {
        scoreDirector.afterListVariableElementUnassigned(variableDescriptor, element);
    }
}