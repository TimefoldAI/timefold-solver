package ai.timefold.solver.core.impl.domain.variable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.list.UnassignedLocation;

final class ExternalizedListVariableDataSupply<Solution_>
        implements ListVariableDataSupply<Solution_> {

    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;
    private Map<Object, ElementLocation> elementLocationMap;
    private int notAssignedElementCount;

    public ExternalizedListVariableDataSupply(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public void resetWorkingSolution(ScoreDirector<Solution_> scoreDirector) {
        this.elementLocationMap = new IdentityHashMap<>();
        var workingSolution = scoreDirector.getWorkingSolution();
        // Start with everything unassigned.
        notAssignedElementCount = (int) sourceVariableDescriptor.getValueRangeSize(workingSolution, null);
        // Will run over all entities and unmark all present elements as unassigned.
        sourceVariableDescriptor.getEntityDescriptor().visitAllEntities(workingSolution, this::insert);
    }

    private void insert(Object entity) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var index = 0;
        for (var element : assignedElements) {
            var oldLocation = elementLocationMap.put(element, new LocationInList(entity, index));
            if (oldLocation != null) {
                throw new IllegalStateException(
                        "The supply (%s) is corrupted, because the element (%s) at index (%d) already exists (%s)."
                                .formatted(this, element, index, oldLocation));
            }
            index++;
            notAssignedElementCount--;
        }
    }

    @Override
    public void close() {
        elementLocationMap = null;
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object o) {
    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object o) {
        insert(o);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object o) {
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object o) {
        // When the entity is removed, its values become unassigned.
        // An unassigned value has no inverse entity and no index.
        retract(o);
    }

    private void retract(Object entity) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var index = 0;
        for (var element : assignedElements) {
            var oldElementLocation = elementLocationMap.put(element, ElementLocation.unassigned());
            if (oldElementLocation == null) {
                throw new IllegalStateException(
                        "The supply (%s) is corrupted, because the element (%s) at index (%d) did not exist."
                                .formatted(this, element, index));
            } else if (oldElementLocation instanceof LocationInList oldLocationInlist) {
                var oldIndex = oldLocationInlist.index();
                if (oldIndex != index) {
                    throw new IllegalStateException(
                            "The supply (%s) is corrupted, because the element (%s) at index (%d) had an old index (%d) which is not the current index (%d)."
                                    .formatted(this, element, index, oldIndex, index));
                }
            } else {
                throw new IllegalStateException(
                        "The supply (%s) is corrupted, because the element (%s) at index (%d) was already unassigned (%s)."
                                .formatted(this, element, index, oldElementLocation));
            }
            index++;
            notAssignedElementCount++;
        }
    }

    @Override
    public void afterListVariableElementUnassigned(ScoreDirector<Solution_> scoreDirector, Object element) {
        var oldLocation = elementLocationMap.remove(element);
        if (oldLocation == null) {
            throw new IllegalStateException(
                    "The supply (%s) is corrupted, because the element (%s) did not exist before unassigning."
                            .formatted(this, element));
        }
        notAssignedElementCount++;
    }

    @Override
    public void beforeListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object o, int fromIndex, int toIndex) {
    }

    @Override
    public void afterListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object o, int fromIndex, int toIndex) {
        updateIndexes(o, fromIndex);
    }

    private void updateIndexes(Object entity, int startIndex) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        for (var index = startIndex; index < assignedElements.size(); index++) {
            var element = assignedElements.get(index);
            var oldLocation = elementLocationMap.put(element, new LocationInList(entity, index));
            if (oldLocation == null || oldLocation instanceof UnassignedLocation) {
                // Fixes the lack of before/afterListVariableElementAssigned().
                notAssignedElementCount--;
            }
            // The first element is allowed to have a null oldIndex because it might have been just assigned.
            if (oldLocation == null && index != startIndex) {
                throw new IllegalStateException(
                        "The supply (%s) is corrupted, because the element (%s) at index (%d) did not exist before."
                                .formatted(this, element, index));
            }
        }
    }

    @Override
    public ElementLocation getLocationInList(Object planningValue) {
        return Objects.requireNonNullElse(elementLocationMap.get(Objects.requireNonNull(planningValue)),
                ElementLocation.unassigned());
    }

    @Override
    public Integer getIndex(Object planningValue) {
        var elementLocation = elementLocationMap.get(Objects.requireNonNull(planningValue));
        if (elementLocation == null) {
            return null;
        }
        return elementLocation instanceof LocationInList elementLocationInList ? elementLocationInList.index() : null;
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        var elementLocation = elementLocationMap.get(Objects.requireNonNull(planningValue));
        if (elementLocation == null) {
            return null;
        }
        return elementLocation instanceof LocationInList elementLocationInList ? elementLocationInList.entity() : null;
    }

    @Override
    public boolean isAssigned(Object element) {
        return getLocationInList(element) instanceof LocationInList;
    }

    @Override
    public int countUnassigned() {
        return notAssignedElementCount;
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
