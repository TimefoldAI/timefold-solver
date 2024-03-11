package ai.timefold.solver.core.impl.heuristic.move;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ListVariableBeforeChangeAction<Solution_, Entity_, Value_> implements ChangeAction<Solution_> {

    private final Entity_ entity;
    private final List<Value_> oldValue;
    private final int fromIndex;
    private final int toIndex;
    private final ListVariableDescriptor<Solution_> variableDescriptor;

    ListVariableBeforeChangeAction(Entity_ entity, List<Value_> oldValue, int fromIndex, int toIndex,
            ListVariableDescriptor<Solution_> variableDescriptor) {
        this.entity = entity;
        this.oldValue = oldValue;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    public void undo(InnerScoreDirector<Solution_, ?> scoreDirector) {
        variableDescriptor.getValue(entity).addAll(fromIndex, oldValue);
        scoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);

        // List variable listeners get confused if there are two pairs of before/after calls
        // before variable listeners are triggered
        scoreDirector.triggerVariableListeners();
    }
}