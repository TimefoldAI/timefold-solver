package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

import org.jspecify.annotations.NonNull;

final class ExternalizedListVariableStateSupply<Solution_>
        implements ListVariableStateSupply<Solution_> {

    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;
    private final ListVariableState<Solution_> listVariableState;

    private boolean previousExternalized = false;
    private boolean nextExternalized = false;
    private Solution_ workingSolution;

    public ExternalizedListVariableStateSupply(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
        this.listVariableState = new ListVariableState<>(sourceVariableDescriptor);
    }

    @Override
    public void externalize(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        listVariableState.linkShadowVariable(shadowVariableDescriptor);
    }

    @Override
    public void externalize(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        listVariableState.linkShadowVariable(shadowVariableDescriptor);
    }

    @Override
    public void externalize(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        listVariableState.linkShadowVariable(shadowVariableDescriptor);
        previousExternalized = true;
    }

    @Override
    public void externalize(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        listVariableState.linkShadowVariable(shadowVariableDescriptor);
        nextExternalized = true;
    }

    @Override
    public void resetWorkingSolution(@NonNull ScoreDirector<Solution_> scoreDirector) {
        workingSolution = scoreDirector.getWorkingSolution();
        listVariableState.initialize((InnerScoreDirector<Solution_, ?>) scoreDirector,
                (int) sourceVariableDescriptor.getValueRangeSize(workingSolution, null));
        // Will run over all entities and unmark all present elements as unassigned.
        sourceVariableDescriptor.getEntityDescriptor()
                .visitAllEntities(workingSolution, this::insert);
    }

    private void insert(Object entity) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var index = 0;
        for (var element : assignedElements) {
            listVariableState.addElement(entity, assignedElements, element, index);
            index++;
        }
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        insert(entity);
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // When the entity is removed, its values become unassigned.
        // An unassigned value has no inverse entity and no index.
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        for (var index = 0; index < assignedElements.size(); index++) {
            listVariableState.removeElement(entity, assignedElements.get(index), index);
        }
    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object element) {
        listVariableState.unassignElement(element);
    }

    @Override
    public void beforeListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity,
            int fromIndex, int toIndex) {
        // No need to do anything.
    }

    @Override
    public void afterListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity, int fromIndex,
            int toIndex) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var elementCount = assignedElements.size();
        // Include the last element of the previous part of the list, if any, for the next element shadow var.
        // But only if the next element shadow var is externalized; otherwise, there is nothing to update.
        var firstChangeIndex = nextExternalized ? Math.max(0, fromIndex - 1) : fromIndex;
        // Include the first element of the next part of the list, if any, for the previous element shadow var.
        // But only if the previous element shadow var is externalized; otherwise, there is nothing to update.
        var lastChangeIndex = previousExternalized ? Math.min(toIndex + 1, elementCount) : toIndex;
        for (var index = firstChangeIndex; index < elementCount; index++) {
            var positionsDiffer = listVariableState.changeElement(entity, assignedElements, index);
            if (!positionsDiffer && index >= lastChangeIndex) {
                // Position is unchanged and we are past the part of the list that changed.
                // We can terminate the loop prematurely.
                return;
            }
        }
    }

    @Override
    public ElementPosition getElementPosition(Object planningValue) {
        return listVariableState.getElementPosition(planningValue);
    }

    @Override
    public Integer getIndex(Object planningValue) {
        return listVariableState.getIndex(planningValue);
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        return listVariableState.getInverseSingleton(planningValue);
    }

    @Override
    public boolean isAssigned(Object element) {
        return getInverseSingleton(element) != null;
    }

    @Override
    public boolean isPinned(Object element) {
        if (!sourceVariableDescriptor.supportsPinning()) {
            return false;
        }
        var position = getElementPosition(element);
        if (position instanceof PositionInList assignedPosition) {
            return sourceVariableDescriptor.isElementPinned(workingSolution, assignedPosition.entity(),
                    assignedPosition.index());
        } else {
            return false;
        }
    }

    @Override
    public int getUnassignedCount() {
        return listVariableState.getUnassignedCount();
    }

    @Override
    public Object getPreviousElement(Object element) {
        return listVariableState.getPreviousElement(element);
    }

    @Override
    public Object getNextElement(Object element) {
        return listVariableState.getNextElement(element);
    }

    @Override
    public ListVariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return sourceVariableDescriptor;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceVariableDescriptor.getVariableName() + ")";
    }

}
