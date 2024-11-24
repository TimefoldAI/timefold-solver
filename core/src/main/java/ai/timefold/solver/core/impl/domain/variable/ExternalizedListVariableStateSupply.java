package ai.timefold.solver.core.impl.domain.variable;

import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.preview.api.domain.metamodel.LocationInList;

import org.jspecify.annotations.NonNull;

final class ExternalizedListVariableStateSupply<Solution_>
        implements ListVariableStateSupply<Solution_> {

    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;
    private IndexVariableProcessor<Solution_> indexProcessor =
            new InternalIndexVariableProcessor<>(this::getIndexFromElementLocationMap);
    private SingletonListInverseVariableProcessor<Solution_> inverseProcessor =
            new InternalSingletonListInverseVariableProcessor<>(this::getInverseFromElementLocationMap);
    private NextPrevElementVariableProcessor<Solution_> previousElementProcessor =
            new InternalNextPrevVariableProcessor<>(this::getPreviousElementFromElementLocationMap);
    private NextPrevElementVariableProcessor<Solution_> nextElementProcessor =
            new InternalNextPrevVariableProcessor<>(this::getNextElementFromElementLocationMap);
    private boolean readLocationFromMap = true;
    private boolean requiresLocationMap = true;
    private Map<Object, LocationInList> elementLocationMap;
    private int unassignedCount;

    public ExternalizedListVariableStateSupply(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    private Integer getIndexFromElementLocationMap(Object planningValue) {
        var elementLocation = getElementLocation(planningValue);
        if (elementLocation == null) {
            return null;
        }
        return elementLocation.index();
    }

    private LocationInList getElementLocation(Object planningValue) {
        return elementLocationMap.get(Objects.requireNonNull(planningValue));
    }

    private Object getInverseFromElementLocationMap(Object planningValue) {
        var elementLocation = getElementLocation(planningValue);
        if (elementLocation == null) {
            return null;
        }
        return elementLocation.entity();
    }

    private Object getPreviousElementFromElementLocationMap(Object planningValue) {
        var elementLocation = getElementLocation(planningValue);
        if (elementLocation == null) {
            return null;
        }
        var index = elementLocation.index();
        if (index == 0) {
            return null;
        }
        return sourceVariableDescriptor.getValue(elementLocation.entity()).get(index - 1);
    }

    private Object getNextElementFromElementLocationMap(Object planningValue) {
        var elementLocation = getElementLocation(planningValue);
        if (elementLocation == null) {
            return null;
        }
        var list = sourceVariableDescriptor.getValue(elementLocation.entity());
        var index = elementLocation.index();
        if (index == list.size() - 1) {
            return null;
        }
        return list.get(index + 1);
    }

    @Override
    public void externalizeIndexVariable(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.indexProcessor = new ExternalizedIndexVariableProcessor<>(shadowVariableDescriptor);
        this.readLocationFromMap = inverseProcessor instanceof InternalSingletonListInverseVariableProcessor<Solution_>;
        this.requiresLocationMap = readLocationFromMap
                || previousElementProcessor instanceof InternalNextPrevVariableProcessor
                || nextElementProcessor instanceof InternalNextPrevVariableProcessor;
    }

    @Override
    public void externalizeSingletonListInverseVariable(
            InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.inverseProcessor =
                new ExternalizedSingletonListInverseVariableProcessor<>(shadowVariableDescriptor, sourceVariableDescriptor);
        this.readLocationFromMap = indexProcessor instanceof InternalIndexVariableProcessor<Solution_>;
        this.requiresLocationMap = readLocationFromMap
                || previousElementProcessor instanceof InternalNextPrevVariableProcessor
                || nextElementProcessor instanceof InternalNextPrevVariableProcessor;
    }

    @Override
    public void externalizePreviousElementShadowVariable(
            PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.previousElementProcessor =
                new ExternalizedPreviousElementVariableProcessor<>(shadowVariableDescriptor);
        this.requiresLocationMap =
                readLocationFromMap || nextElementProcessor instanceof InternalNextPrevVariableProcessor;
    }

    @Override
    public void externalizeNextElementShadowVariable(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.nextElementProcessor = new ExternalizedNextElementVariableProcessor<>(shadowVariableDescriptor);
        this.requiresLocationMap =
                readLocationFromMap || previousElementProcessor instanceof InternalNextPrevVariableProcessor;
    }

    @Override
    public void resetWorkingSolution(@NonNull ScoreDirector<Solution_> scoreDirector) {
        var workingSolution = scoreDirector.getWorkingSolution();
        // Start with everything unassigned.
        this.unassignedCount = (int) sourceVariableDescriptor.getValueRangeSize(workingSolution, null);
        if (requiresLocationMap) {
            if (elementLocationMap == null) {
                elementLocationMap = CollectionUtils.newIdentityHashMap(unassignedCount);
            } else {
                elementLocationMap.clear();
            }
        } else {
            elementLocationMap = null;
        }
        // Will run over all entities and unmark all present elements as unassigned.
        sourceVariableDescriptor.getEntityDescriptor()
                .visitAllEntities(workingSolution, o -> insert(scoreDirector, o));
    }

    private void insert(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var trackLocation = requiresLocationMap;
        var index = 0;
        for (var element : assignedElements) {
            if (trackLocation) {
                var location = ElementLocation.of(entity, index);
                var oldLocation = elementLocationMap.put(element, location);
                if (oldLocation != null) {
                    throw new IllegalStateException(
                            "The supply (%s) is corrupted, because the element (%s) at index (%d) already exists (%s)."
                                    .formatted(this, element, index, oldLocation));
                }
            }
            var castScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
            indexProcessor.addElement(castScoreDirector, element, index);
            inverseProcessor.addElement(castScoreDirector, entity, element);
            previousElementProcessor.setElement(castScoreDirector, assignedElements, element, index);
            nextElementProcessor.setElement(castScoreDirector, assignedElements, element, index);
            index++;
            unassignedCount--;
        }
    }

    @Override
    public void close() {
        elementLocationMap = null;
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o) {
        insert(scoreDirector, o);
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o) {
        // When the entity is removed, its values become unassigned.
        // An unassigned value has no inverse entity and no index.
        retract(scoreDirector, o);
    }

    private void retract(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var trackLocation = requiresLocationMap;
        for (var index = 0; index < assignedElements.size(); index++) {
            var element = assignedElements.get(index);
            if (trackLocation) {
                var oldElementLocation = elementLocationMap.remove(element);
                if (oldElementLocation == null) {
                    throw new IllegalStateException(
                            "The supply (%s) is corrupted, because the element (%s) at index (%d) was already unassigned (%s)."
                                    .formatted(this, element, index, oldElementLocation));
                }
                var oldIndex = oldElementLocation.index();
                if (oldIndex != index) {
                    throw new IllegalStateException(
                            "The supply (%s) is corrupted, because the element (%s) at index (%d) had an old index (%d) which is not the current index (%d)."
                                    .formatted(this, element, index, oldIndex, index));
                }
            }
            var castScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
            indexProcessor.removeElement(castScoreDirector, element);
            inverseProcessor.removeElement(castScoreDirector, entity, element);
            previousElementProcessor.unsetElement(castScoreDirector, element);
            nextElementProcessor.unsetElement(castScoreDirector, element);
            unassignedCount++;
        }
    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object element) {
        if (requiresLocationMap) {
            var oldLocation = elementLocationMap.remove(element);
            if (oldLocation == null) {
                throw new IllegalStateException(
                        "The supply (%s) is corrupted, because the element (%s) did not exist before unassigning."
                                .formatted(this, element));
            }
        }
        var castScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        indexProcessor.unassignElement(castScoreDirector, element);
        inverseProcessor.unassignElement(castScoreDirector, element);
        previousElementProcessor.unsetElement(castScoreDirector, element);
        nextElementProcessor.unsetElement(castScoreDirector, element);
        unassignedCount++;
    }

    @Override
    public void beforeListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o, int fromIndex,
            int toIndex) {
        // No need to do anything.
    }

    @Override
    public void afterListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o, int fromIndex,
            int toIndex) {
        var castScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        var assignedElements = sourceVariableDescriptor.getValue(o);
        if (fromIndex > 0) {
            // Include the last element of the previous part of the list too.
            // Otherwise the last element would point to the wrong next element.
            var previousIndex = fromIndex - 1;
            nextElementProcessor.setElement(castScoreDirector, assignedElements, assignedElements.get(previousIndex),
                    previousIndex);
        }
        var elementCount = assignedElements.size();
        var trackLocation = requiresLocationMap;
        for (var index = fromIndex; index < elementCount; index++) {
            var element = assignedElements.get(index);

            boolean locationsDiffer;
            if (trackLocation) { // Update the location and figure out if it is different from previous.
                var newLocation = ElementLocation.of(o, index);
                var oldLocation = elementLocationMap.put(element, newLocation);
                if (oldLocation == null) {
                    unassignedCount--;
                }
                locationsDiffer = !newLocation.equals(oldLocation);
            } else { // Read the location and figure out if it is different from previous.
                var previousEntity = getInverseSingleton(element);
                if (previousEntity == null) {
                    unassignedCount--;
                }
                locationsDiffer = previousEntity != o || getIndex(element) != index;
            }
            // Update location; no-op if the map is used.
            indexProcessor.changeElement(castScoreDirector, element, index);
            inverseProcessor.changeElement(castScoreDirector, o, element);
            // Update previous and next elements; no-op if the map is used.
            previousElementProcessor.setElement(castScoreDirector, assignedElements, element, index);
            nextElementProcessor.setElement(castScoreDirector, assignedElements, element, index);

            if (index >= toIndex && !locationsDiffer) {
                // Location is unchanged and we are past the part of the list that changed.
                if (index < elementCount - 1) {
                    // Include the last element of the previous part of the list too.
                    // Otherwise the last element would point to the wrong next element.
                    var nextIndex = index + 1;
                    previousElementProcessor.setElement(castScoreDirector, assignedElements, assignedElements.get(nextIndex),
                            nextIndex);
                }
                // Finally, we can terminate the loop prematurely.
                return;
            } else {
                // Continue to the next element.
            }
        }
    }

    @Override
    public ElementLocation getLocationInList(Object planningValue) {
        if (readLocationFromMap) {
            return Objects.requireNonNullElse(getElementLocation(planningValue), ElementLocation.unassigned());
        } else {
            var inverse = getInverseSingleton(planningValue);
            if (inverse == null) {
                return ElementLocation.unassigned();
            }
            return ElementLocation.of(inverse, getIndex(planningValue));
        }
    }

    @Override
    public Integer getIndex(Object planningValue) {
        return indexProcessor.getIndex(planningValue);
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        return inverseProcessor.getInverseSingleton(planningValue);
    }

    @Override
    public boolean isAssigned(Object element) {
        return getInverseSingleton(element) != null;
    }

    @Override
    public int getUnassignedCount() {
        return unassignedCount;
    }

    @Override
    public Object getPreviousElement(Object element) {
        return previousElementProcessor.getElement(element);
    }

    @Override
    public Object getNextElement(Object element) {
        return nextElementProcessor.getElement(element);
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
