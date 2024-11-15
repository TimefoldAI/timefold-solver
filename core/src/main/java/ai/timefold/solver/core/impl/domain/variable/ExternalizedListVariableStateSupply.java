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
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedLocation;

import org.jspecify.annotations.NonNull;

final class ExternalizedListVariableStateSupply<Solution_>
        implements ListVariableStateSupply<Solution_> {

    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;
    private IndexVariableProcessor<Solution_> indexProcessor =
            new InternalIndexVariableProcessor<>(this::getIndexFromElementLocationMap);
    private SingletonListInverseVariableProcessor<Solution_> inverseProcessor =
            new InternalSingletonListListInverseVariableProcessor<>(this::getInverseFromElementLocationMap);
    private PreviousElementVariableProcessor<Solution_> previousElementProcessor;
    private NextElementVariableProcessor<Solution_> nextElementProcessor;
    private boolean requiresLocationTracking = true;
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

    @Override
    public void externalizeIndexVariable(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.indexProcessor = new ExternalizedIndexVariableProcessor<>(shadowVariableDescriptor);
        this.requiresLocationTracking = inverseProcessor instanceof InternalSingletonListListInverseVariableProcessor;
    }

    @Override
    public void externalizeSingletonListInverseVariable(
            InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.inverseProcessor =
                new ExternalizedSingletonListListInverseVariableProcessor<>(shadowVariableDescriptor, sourceVariableDescriptor);
        this.requiresLocationTracking = indexProcessor instanceof InternalIndexVariableProcessor<Solution_>;
    }

    @Override
    public void
            enablePreviousElementShadowVariable(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.previousElementProcessor =
                new PreviousElementVariableProcessor<>(shadowVariableDescriptor);
    }

    private boolean isPreviousElementShadowVariableEnabled() {
        return previousElementProcessor != null;
    }

    private boolean isNextElementShadowVariableEnabled() {
        return nextElementProcessor != null;
    }

    @Override
    public void enableNextElementShadowVariable(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.nextElementProcessor = new NextElementVariableProcessor<>(shadowVariableDescriptor);
    }

    @Override
    public void resetWorkingSolution(@NonNull ScoreDirector<Solution_> scoreDirector) {
        var workingSolution = scoreDirector.getWorkingSolution();
        // Start with everything unassigned.
        this.unassignedCount = (int) sourceVariableDescriptor.getValueRangeSize(workingSolution, null);
        if (requiresLocationTracking) {
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
                .visitAllEntities(workingSolution, o -> insert(scoreDirector, o, isPreviousElementShadowVariableEnabled(),
                        isNextElementShadowVariableEnabled()));
    }

    private void insert(ScoreDirector<Solution_> scoreDirector, Object entity, boolean previousElementProcessingEnabled,
            boolean nextElementProcessingEnabled) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var trackLocation = requiresLocationTracking;
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
            if (previousElementProcessingEnabled) {
                previousElementProcessor.addElement(castScoreDirector, assignedElements, element, index);
            }
            if (nextElementProcessingEnabled) {
                nextElementProcessor.addElement(castScoreDirector, assignedElements, element, index);
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
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o) {
        insert(scoreDirector, o, isPreviousElementShadowVariableEnabled(), isNextElementShadowVariableEnabled());
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object o) {
        // When the entity is removed, its values become unassigned.
        // An unassigned value has no inverse entity and no index.
        retract(scoreDirector, o, isPreviousElementShadowVariableEnabled(), isNextElementShadowVariableEnabled());
    }

    private void retract(ScoreDirector<Solution_> scoreDirector, Object entity, boolean previousElementProcessingEnabled,
            boolean nextElementProcessingEnabled) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var trackLocation = requiresLocationTracking;
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
            if (previousElementProcessingEnabled) {
                previousElementProcessor.removeElement(castScoreDirector, element);
            }
            if (nextElementProcessingEnabled) {
                nextElementProcessor.removeElement(castScoreDirector, element);
            }
            unassignedCount++;
        }
    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object element) {
        if (requiresLocationTracking) {
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
        if (isPreviousElementShadowVariableEnabled()) {
            previousElementProcessor.removeElement(castScoreDirector, element);
        }
        if (isNextElementShadowVariableEnabled()) {
            nextElementProcessor.removeElement(castScoreDirector, element);
        }
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
        var previousElementProcessingEnabled = isPreviousElementShadowVariableEnabled();
        var nextElementProcessingEnabled = isNextElementShadowVariableEnabled();
        if (nextElementProcessingEnabled && fromIndex > 0) {
            // If we need to process next elements, include the last element of the previous part of the list too.
            // Otherwise the last element would point to the wrong next element.
            var previousIndex = fromIndex - 1;
            nextElementProcessor.changeElement(castScoreDirector, assignedElements, assignedElements.get(previousIndex),
                    previousIndex);
        }
        var elementCount = assignedElements.size();
        var trackLocation = requiresLocationTracking;
        for (var index = fromIndex; index < elementCount; index++) {
            var element = assignedElements.get(index);
            var locationsDiffer = changeLocation(o, element, index, trackLocation);
            indexProcessor.changeElement(castScoreDirector, element, index);
            inverseProcessor.changeElement(castScoreDirector, o, element);
            if (previousElementProcessingEnabled) {
                previousElementProcessor.changeElement(castScoreDirector, assignedElements, element, index);
            }
            if (nextElementProcessingEnabled) {
                nextElementProcessor.changeElement(castScoreDirector, assignedElements, element, index);
            }
            if (index >= toIndex && !locationsDiffer) {
                // Location is unchanged and we are past the part of the list that changed.
                // If we need to process previous elements, include the last element of the previous part of the list too.
                // Otherwise the last element would point to the wrong next element.
                if (previousElementProcessingEnabled && index < elementCount - 1) {
                    var nextIndex = index + 1;
                    previousElementProcessor.changeElement(castScoreDirector, assignedElements, assignedElements.get(nextIndex),
                            nextIndex);
                }
                // Finally, we can terminate the loop prematurely.
                return;
            } else {
                // Continue to the next element.
            }
        }
    }

    private boolean changeLocation(Object entity, Object element, int index, boolean trackLocation) {
        var newLocation = ElementLocation.of(entity, index);
        var oldLocation = trackLocation ? elementLocationMap.put(element, newLocation)
                : getLocationInList(element);
        if (oldLocation == null || oldLocation instanceof UnassignedLocation) {
            unassignedCount--;
            return true;
        } else {
            return !oldLocation.equals(newLocation);
        }
    }

    @Override
    public ElementLocation getLocationInList(Object planningValue) {
        var inverse = getInverseSingleton(planningValue);
        if (inverse == null) {
            return ElementLocation.unassigned();
        }
        return ElementLocation.of(inverse, getIndex(planningValue));
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
    public ListVariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return sourceVariableDescriptor;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceVariableDescriptor.getVariableName() + ")";
    }

}
