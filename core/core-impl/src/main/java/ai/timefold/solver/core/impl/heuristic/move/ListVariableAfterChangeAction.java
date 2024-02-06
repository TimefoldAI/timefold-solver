package ai.timefold.solver.core.impl.heuristic.move;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ListVariableAfterChangeAction<Solution_, Entity_, Value_> implements ChangeAction<Solution_> {

    private final Entity_ entity;
    private final int fromIndex;
    private final int toIndex;
    private final ListVariableDescriptor<Solution_> variableDescriptor;

    ListVariableAfterChangeAction(Entity_ entity, int fromIndex, int toIndex,
            ListVariableDescriptor<Solution_> variableDescriptor) {
        this.entity = entity;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    public void undo(InnerScoreDirector<Solution_, ?> scoreDirector) {
        scoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        @SuppressWarnings("unchecked")
        var items = (List<Value_>) variableDescriptor.getValue(entity).subList(fromIndex, toIndex);
        items.clear();
    }

}