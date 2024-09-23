package ai.timefold.solver.core.impl.move.director;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

record ListVariableBeforeChangeAction<Solution_, Entity_, Value_>(Entity_ entity, List<Value_> oldValue, int fromIndex,
        int toIndex, ListVariableDescriptor<Solution_> variableDescriptor) implements ChangeAction<Solution_> {

    @Override
    public void undo(InnerScoreDirector<Solution_, ?> scoreDirector) {
        variableDescriptor.getValue(entity).addAll(fromIndex, oldValue);
        scoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);

        // List variable listeners get confused if there are two pairs of before/after calls
        // before variable listeners are triggered
        scoreDirector.triggerVariableListeners();
    }

}