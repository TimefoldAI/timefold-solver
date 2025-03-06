package ai.timefold.solver.core.impl.move.director;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Rebaser;

record ListVariableBeforeChangeAction<Solution_, Entity_, Value_>(Entity_ entity, List<Value_> oldValue, int fromIndex,
        int toIndex, ListVariableDescriptor<Solution_> variableDescriptor) implements ChangeAction<Solution_> {

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        variableDescriptor.getValue(entity).addAll(fromIndex, oldValue);
        scoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public ChangeAction<Solution_> rebase(Rebaser rebaser) {
        var rebasedValueList = oldValue().stream().map(rebaser::rebase).toList();
        return new ListVariableBeforeChangeAction<>(rebaser.rebase(entity), rebasedValueList, fromIndex, toIndex,
                variableDescriptor);
    }

}