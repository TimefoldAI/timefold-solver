package ai.timefold.solver.core.impl.move.director;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

final class DefaultActionRecorder<Solution_> implements ActionRecorder<Solution_> {

    private final List<ChangeAction<Solution_>> variableChanges;

    DefaultActionRecorder() {
        // Intentional LinkedList; fast clear, no allocations upfront,
        // will most often only carry a small number of items.
        this.variableChanges = new LinkedList<>();
    }

    List<ChangeAction<Solution_>> copy() {
        return List.copyOf(variableChanges);
    }

    void clear() {
        variableChanges.clear();
    }

    int size() {
        return variableChanges.size();
    }

    ListIterator<ChangeAction<Solution_>> iterator(int count) {
        return variableChanges.listIterator(count);
    }

    @Override
    public void recordVariableChangeAction(VariableDescriptor<Solution_> variableDescriptor, Object entity, Object value) {
        variableChanges.add(new VariableChangeAction<>(entity, value, variableDescriptor));
    }

    @Override
    public void recordListVariableBeforeChangeAction(ListVariableDescriptor<Solution_> variableDescriptor, Object entity,
            List<Object> values, int fromIndex, int toIndex) {
        variableChanges.add(new ListVariableBeforeChangeAction<>(entity, values, fromIndex, toIndex, variableDescriptor));
    }

    @Override
    public void recordListVariableAfterChangeAction(ListVariableDescriptor<Solution_> variableDescriptor, Object entity,
            int fromIndex, int toIndex) {
        variableChanges.add(new ListVariableAfterChangeAction<>(entity, fromIndex, toIndex, variableDescriptor));
    }

    @Override
    public void recordListVariableBeforeAssignmentAction(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableBeforeAssignmentAction<>(element, variableDescriptor));
    }

    @Override
    public void recordListVariableAfterAssignmentAction(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableAfterAssignmentAction<>(element, variableDescriptor));
    }

    @Override
    public void recordListVariableBeforeUnassignmentAction(ListVariableDescriptor<Solution_> variableDescriptor,
            Object element) {
        variableChanges.add(new ListVariableBeforeUnassignmentAction<>(element, variableDescriptor));
    }

    @Override
    public void recordListVariableAfterUnassignmentAction(ListVariableDescriptor<Solution_> variableDescriptor,
            Object element) {
        variableChanges.add(new ListVariableAfterUnassignmentAction<>(element, variableDescriptor));
    }
}
