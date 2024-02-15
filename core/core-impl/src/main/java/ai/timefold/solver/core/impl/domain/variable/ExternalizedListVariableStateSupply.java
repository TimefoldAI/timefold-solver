package ai.timefold.solver.core.impl.domain.variable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;

final class ExternalizedListVariableStateSupply<Solution_>
        implements ListVariableStateSupply<Solution_> {

    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;
    private Map<Object, ElementLocation> elementLocationMap;
    private int unassignedCount;

    public ExternalizedListVariableStateSupply(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public void resetWorkingSolution(ScoreDirector<Solution_> scoreDirector) {
        this.elementLocationMap = new IdentityHashMap<>();
        var workingSolution = scoreDirector.getWorkingSolution();
        // Start with everything unassigned.
        unassignedCount = (int) sourceVariableDescriptor.getValueRangeSize(workingSolution, null);
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
            unassignedCount--;
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
        for (var index = 0; index < assignedElements.size(); index++) {
            var element = assignedElements.get(index);
            var oldElementLocation = elementLocationMap.remove(element);
            if (oldElementLocation instanceof LocationInList oldLocationInlist) {
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
            unassignedCount++;
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
        unassignedCount++;
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
            if (!(oldLocation instanceof LocationInList)) {
                // Fixes the lack of before/afterListVariableElementAssigned().
                unassignedCount--;
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
    public int getUnassignedCount() {
        return unassignedCount;
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
