package ai.timefold.solver.core.impl.domain.variable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.preview.api.domain.metamodel.LocationInList;

import org.jspecify.annotations.NonNull;

final class ExternalizedListVariableStateSupply<Solution_>
        implements ListVariableStateSupply<Solution_> {

    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;
    private IndexVariableProcessor<Solution_> indexProcessor = new ExternalizedIndexVariableProcessor<>(planningValue -> {
        var elementLocation = getElementLocation(planningValue);
        if (elementLocation == null) {
            return null;
        }
        return elementLocation.index();
    });
    private SingletonListInverseVariableProcessor<Solution_> inverseProcessor =
            new ExternalizedSingletonListListInverseVariableProcessor<>(planningValue -> {
                var elementLocation = getElementLocation(planningValue);
                if (elementLocation == null) {
                    return null;
                }
                return elementLocation.entity();
            });
    private PreviousElementVariableProcessor<Solution_> previousElementProcessor;
    private NextElementVariableProcessor<Solution_> nextElementProcessor;
    private Map<Object, LocationInList> elementLocationMap;
    private int unassignedCount;

    public ExternalizedListVariableStateSupply(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public void externalizeIndexVariable(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.indexProcessor = new InternalIndexVariableProcessor<>(shadowVariableDescriptor);
    }

    @Override
    public void externalizeSingletonListInverseVariable(
            InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.inverseProcessor =
                new InternalSingletonListListInverseVariableProcessor<>(shadowVariableDescriptor, sourceVariableDescriptor);
    }

    @Override
    public void
            enablePreviousElementShadowVariable(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.previousElementProcessor =
                new PreviousElementVariableProcessor<>(shadowVariableDescriptor);
    }

    @Override
    public void enableNextElementShadowVariable(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.nextElementProcessor = new NextElementVariableProcessor<>(shadowVariableDescriptor);
    }

    private LocationInList getElementLocation(Object planningValue) {
        return elementLocationMap.get(Objects.requireNonNull(planningValue));
    }

    @Override
    public void resetWorkingSolution(@NonNull ScoreDirector<Solution_> scoreDirector) {
        var workingSolution = scoreDirector.getWorkingSolution();
        if (elementLocationMap == null) {
            elementLocationMap = new IdentityHashMap<>((int) sourceVariableDescriptor.getValueRangeSize(workingSolution, null));
        } else {
            elementLocationMap.clear();
        }
        // Start with everything unassigned.
        unassignedCount = (int) sourceVariableDescriptor.getValueRangeSize(workingSolution, null);
        // Will run over all entities and unmark all present elements as unassigned.
        sourceVariableDescriptor.getEntityDescriptor()
                .visitAllEntities(workingSolution, o -> insert(scoreDirector, o));
    }

    private void insert(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var index = 0;
        for (var element : assignedElements) {
            var location = ElementLocation.of(entity, index);
            var oldLocation = elementLocationMap.put(element, location);
            if (oldLocation != null) {
                throw new IllegalStateException(
                        "The supply (%s) is corrupted, because the element (%s) at index (%d) already exists (%s)."
                                .formatted(this, element, index, oldLocation));
            }
            indexProcessor.addElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element, index);
            inverseProcessor.addElement((InnerScoreDirector<Solution_, ?>) scoreDirector, entity, element);
            if (nextElementProcessor != null) {
                nextElementProcessor.addElement((InnerScoreDirector<Solution_, ?>) scoreDirector, assignedElements, element,
                        location);
            }
            if (previousElementProcessor != null) {
                previousElementProcessor.addElement((InnerScoreDirector<Solution_, ?>) scoreDirector, assignedElements, element,
                        location);
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
        for (var index = 0; index < assignedElements.size(); index++) {
            var element = assignedElements.get(index);
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
            indexProcessor.removeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element);
            inverseProcessor.removeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, entity, element);
            if (nextElementProcessor != null) {
                nextElementProcessor.removeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element);
            }
            if (previousElementProcessor != null) {
                previousElementProcessor.removeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element);
            }
            unassignedCount++;
        }
    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object element) {
        var oldLocation = elementLocationMap.remove(element);
        if (oldLocation == null) {
            throw new IllegalStateException(
                    "The supply (%s) is corrupted, because the element (%s) did not exist before unassigning."
                            .formatted(this, element));
        }
        indexProcessor.unassignElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element);
        inverseProcessor.unassignElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element);
        if (nextElementProcessor != null) {
            nextElementProcessor.removeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element);
        }
        if (previousElementProcessor != null) {
            previousElementProcessor.removeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element);
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
        var assignedElements = sourceVariableDescriptor.getValue(o);
        if (nextElementProcessor != null && fromIndex > 0) {
            // If we need to process next elements, include the last element of the previous part of the list too.
            // Otherwise the last element would point to the wrong next element.
            nextElementProcessor.changeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, assignedElements,
                    assignedElements.get(fromIndex - 1),
                    ElementLocation.of(o, fromIndex - 1));
        }
        for (var index = fromIndex; index < assignedElements.size(); index++) {
            var element = assignedElements.get(index);
            var newLocation = ElementLocation.of(o, index);
            var oldLocation = elementLocationMap.put(element, newLocation);
            indexProcessor.changeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, element, index);
            inverseProcessor.changeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, o, element);
            if (nextElementProcessor != null) {
                nextElementProcessor.changeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, assignedElements, element,
                        newLocation);
            }
            if (previousElementProcessor != null) {
                previousElementProcessor.changeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, assignedElements,
                        element,
                        newLocation);
            }
            if (oldLocation == null) {
                unassignedCount--;
            }
            if (index >= toIndex && newLocation.equals(oldLocation)) {
                // Location is unchanged and we are past the part of the list that changed.
                // If we need to process previous elements, include the last element of the previous part of the list too.
                // Otherwise the last element would point to the wrong next element.
                if (previousElementProcessor != null && index < assignedElements.size() - 1) {
                    previousElementProcessor.changeElement((InnerScoreDirector<Solution_, ?>) scoreDirector, assignedElements,
                            assignedElements.get(index + 1), ElementLocation.of(o, index + 1));
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
        return Objects.requireNonNullElse(elementLocationMap.get(Objects.requireNonNull(planningValue)),
                ElementLocation.unassigned());
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
