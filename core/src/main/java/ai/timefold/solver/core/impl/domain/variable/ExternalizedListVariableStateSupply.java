package ai.timefold.solver.core.impl.domain.variable;

import java.util.ArrayList;
import java.util.List;
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

    private ExternalizedIndexVariableProcessor<Solution_> externalizedIndexProcessor = null;
    private ExternalizedSingletonListInverseVariableProcessor<Solution_> externalizedInverseProcessor = null;
    private AbstractExternalizedNextPrevElementVariableProcessor<Solution_> externalizedPreviousElementProcessor = null;
    private AbstractExternalizedNextPrevElementVariableProcessor<Solution_> externalizedNextElementProcessor = null;
    private ElementAdder<Solution_> elementAdder = createElementAdder();
    private ElementRemover<Solution_> elementRemover = createElementRemover();
    private ElementUnassigner<Solution_> elementUnassigner = createElementUnassigner();
    private ElementChanger<Solution_> elementChanger = createElementChanger();
    private boolean canUseExternalizedLocation = false;
    private boolean requiresLocationMap = true;
    private Map<Object, LocationInList> elementLocationMap;
    private int unassignedCount;

    public ExternalizedListVariableStateSupply(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public void externalizeIndexVariable(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedIndexProcessor = new ExternalizedIndexVariableProcessor<>(shadowVariableDescriptor);
        reinitializeAccessors();
    }

    private void reinitializeAccessors() {
        this.canUseExternalizedLocation = externalizedIndexProcessor != null && externalizedInverseProcessor != null;
        this.requiresLocationMap = !canUseExternalizedLocation
                || externalizedPreviousElementProcessor == null || externalizedNextElementProcessor == null;
        this.elementAdder = createElementAdder();
        this.elementRemover = createElementRemover();
        this.elementUnassigner = createElementUnassigner();
        this.elementChanger = createElementChanger();
    }

    @Override
    public void externalizeSingletonListInverseVariable(
            InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedInverseProcessor =
                new ExternalizedSingletonListInverseVariableProcessor<>(shadowVariableDescriptor, sourceVariableDescriptor);
        reinitializeAccessors();
    }

    @Override
    public void externalizePreviousElementShadowVariable(
            PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedPreviousElementProcessor =
                new ExternalizedPreviousElementVariableProcessor<>(shadowVariableDescriptor);
        reinitializeAccessors();
    }

    @Override
    public void externalizeNextElementShadowVariable(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedNextElementProcessor = new ExternalizedNextElementVariableProcessor<>(shadowVariableDescriptor);
        reinitializeAccessors();
    }

    @FunctionalInterface
    private interface ElementAdder<Solution_> {

        void apply(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, List<Object> elements, Object element,
                Integer index);

    }

    private ElementAdder<Solution_> createElementAdder() {
        var list = new ArrayList<ElementAdder<Solution_>>(4);
        if (externalizedIndexProcessor != null) {
            list.add((scoreDirector, entity, elements, element, index) -> externalizedIndexProcessor.addElement(scoreDirector,
                    element, index));
        }
        if (externalizedInverseProcessor != null) {
            list.add((scoreDirector, entity, elements, element, index) -> externalizedInverseProcessor.addElement(scoreDirector,
                    entity, element));
        }
        if (externalizedPreviousElementProcessor != null) {
            list.add((scoreDirector, entity, elements, element, index) -> externalizedPreviousElementProcessor
                    .setElement(scoreDirector, elements, element, index));
        }
        if (externalizedNextElementProcessor != null) {
            list.add((scoreDirector, entity, elements, element, index) -> externalizedNextElementProcessor
                    .setElement(scoreDirector, elements, element, index));
        }

        return switch (list.size()) {
            case 0 -> (scoreDirector, entity, elements, element, index) -> {
                // Do nothing
            };
            case 1 -> list.get(0);
            default -> {
                var array = list.toArray(ElementAdder[]::new);
                yield (scoreDirector, entity, elements, element, index) -> {
                    for (var adder : array) {
                        adder.apply(scoreDirector, entity, elements, element, index);
                    }
                };
            }
        };
    }

    @FunctionalInterface
    private interface ElementRemover<Solution_> {

        void apply(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element);

    }

    private ElementRemover<Solution_> createElementRemover() {
        var list = new ArrayList<ElementRemover<Solution_>>(4);
        if (externalizedIndexProcessor != null) {
            list.add((scoreDirector, entity, element) -> externalizedIndexProcessor.removeElement(scoreDirector, element));
        }
        if (externalizedInverseProcessor != null) {
            list.add((scoreDirector, entity, element) -> externalizedInverseProcessor.removeElement(scoreDirector, entity,
                    element));
        }
        if (externalizedPreviousElementProcessor != null) {
            list.add((scoreDirector, entity, element) -> externalizedPreviousElementProcessor.unsetElement(scoreDirector,
                    element));
        }
        if (externalizedNextElementProcessor != null) {
            list.add((scoreDirector, entity, element) -> externalizedNextElementProcessor.unsetElement(scoreDirector, element));
        }

        return switch (list.size()) {
            case 0 -> (scoreDirector, entity, element) -> {
                // Do nothing
            };
            case 1 -> list.get(0);
            default -> {
                var array = list.toArray(ElementRemover[]::new);
                yield (scoreDirector, entity, element) -> {
                    for (var remover : array) {
                        remover.apply(scoreDirector, entity, element);
                    }
                };
            }
        };
    }

    @FunctionalInterface
    private interface ElementUnassigner<Solution_> {

        void apply(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    }

    private ElementUnassigner<Solution_> createElementUnassigner() {
        var list = new ArrayList<ElementUnassigner<Solution_>>(4);
        if (externalizedIndexProcessor != null) {
            list.add((scoreDirector, element) -> externalizedIndexProcessor.unassignElement(scoreDirector, element));
        }
        if (externalizedInverseProcessor != null) {
            list.add((scoreDirector, element) -> externalizedInverseProcessor.unassignElement(scoreDirector, element));
        }
        if (externalizedPreviousElementProcessor != null) {
            list.add((scoreDirector, element) -> externalizedPreviousElementProcessor.unsetElement(scoreDirector, element));
        }
        if (externalizedNextElementProcessor != null) {
            list.add((scoreDirector, element) -> externalizedNextElementProcessor.unsetElement(scoreDirector, element));
        }

        return switch (list.size()) {
            case 0 -> (scoreDirector, element) -> {
                // Do nothing
            };
            case 1 -> list.get(0);
            default -> {
                var array = list.toArray(ElementUnassigner[]::new);
                yield (scoreDirector, element) -> {
                    for (var unassigner : array) {
                        unassigner.apply(scoreDirector, element);
                    }
                };
            }
        };
    }

    @FunctionalInterface
    private interface ElementChanger<Solution_> {

        void apply(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, List<Object> elements, Object element,
                Integer index);

    }

    private ElementChanger<Solution_> createElementChanger() {
        var list = new ArrayList<ElementChanger<Solution_>>(4);
        if (externalizedIndexProcessor != null) {
            list.add((scoreDirector, entity, elements, element, index) -> externalizedIndexProcessor
                    .changeElement(scoreDirector, element, index));
        }
        if (externalizedInverseProcessor != null) {
            list.add((scoreDirector, entity, elements, element, index) -> externalizedInverseProcessor
                    .changeElement(scoreDirector, entity, element));
        }
        if (externalizedPreviousElementProcessor != null) {
            list.add((scoreDirector, entity, elements, element, index) -> externalizedPreviousElementProcessor
                    .setElement(scoreDirector, elements, element, index));
        }
        if (externalizedNextElementProcessor != null) {
            list.add((scoreDirector, entity, elements, element, index) -> externalizedNextElementProcessor
                    .setElement(scoreDirector, elements, element, index));
        }

        return switch (list.size()) {
            case 0 -> (scoreDirector, entity, elements, element, index) -> {
                // Do nothing
            };
            case 1 -> list.get(0);
            default -> {
                var array = list.toArray(ElementChanger[]::new);
                yield (scoreDirector, entity, elements, element, index) -> {
                    for (var changer : array) {
                        changer.apply(scoreDirector, entity, elements, element, index);
                    }
                };
            }
        };
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
        var castScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var index = 0;
        for (var element : assignedElements) {
            if (requiresLocationMap) {
                var location = ElementLocation.of(entity, index);
                var oldLocation = elementLocationMap.put(element, location);
                if (oldLocation != null) {
                    throw new IllegalStateException(
                            "The supply (%s) is corrupted, because the element (%s) at index (%d) already exists (%s)."
                                    .formatted(this, element, index, oldLocation));
                }
            }
            elementAdder.apply(castScoreDirector, entity, assignedElements, element, index);
            index++;
            unassignedCount--;
        }
    }

    @Override
    public void close() {
        elementLocationMap = null;
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        insert(scoreDirector, entity);
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // When the entity is removed, its values become unassigned.
        // An unassigned value has no inverse entity and no index.
        retract(scoreDirector, entity);
    }

    private void retract(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var castScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        for (var index = 0; index < assignedElements.size(); index++) {
            var element = assignedElements.get(index);
            if (requiresLocationMap) {
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
            elementRemover.apply(castScoreDirector, entity, element);
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
        elementUnassigner.apply((InnerScoreDirector<Solution_, ?>) scoreDirector, element);
        unassignedCount++;
    }

    @Override
    public void beforeListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity,
            int fromIndex, int toIndex) {
        // No need to do anything.
    }

    @Override
    public void afterListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity, int fromIndex,
            int toIndex) {
        var castScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        var assignedElements = sourceVariableDescriptor.getValue(entity);
        var elementCount = assignedElements.size();
        // Include the last element of the previous part of the list, if any, for the next element shadow var.
        var firstChangeIndex = Math.max(0, fromIndex - 1);
        // Include the first element of the next part of the list, if any, for the previous element shadow var.
        var lastChangeIndex = Math.min(toIndex + 1, elementCount);
        for (var index = firstChangeIndex; index < elementCount; index++) {
            var element = assignedElements.get(index);
            var boxedIndex = Integer.valueOf(index); // Avoid many counts of auto-boxing.
            var locationsDiffer = processElementLocation(entity, element, boxedIndex);
            elementChanger.apply(castScoreDirector, entity, assignedElements, element, boxedIndex);
            if (!locationsDiffer && index >= lastChangeIndex) {
                // Location is unchanged and we are past the part of the list that changed.
                // We can terminate the loop prematurely.
                return;
            }
        }
    }

    private boolean processElementLocation(Object entity, Object element, Integer index) {
        if (requiresLocationMap) { // Update the location and figure out if it is different from previous.
            var newLocation = ElementLocation.of(entity, index);
            var oldLocation = elementLocationMap.put(element, newLocation);
            if (oldLocation == null) {
                unassignedCount--;
                return true;
            }
            return !newLocation.equals(oldLocation);
        } else { // Read the location and figure out if it is different from previous.
            var previousEntity = getInverseSingleton(element);
            if (previousEntity == null) {
                unassignedCount--;
                return true;
            }
            return previousEntity != entity || !Objects.equals(getIndex(element), index);
        }
    }

    @Override
    public ElementLocation getLocationInList(Object planningValue) {
        if (!canUseExternalizedLocation) {
            return Objects.requireNonNullElse(elementLocationMap.get(planningValue), ElementLocation.unassigned());
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
        if (externalizedIndexProcessor == null) {
            var elementLocation = elementLocationMap.get(planningValue);
            if (elementLocation == null) {
                return null;
            }
            return elementLocation.index();
        }
        return externalizedIndexProcessor.getIndex(planningValue);
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        if (externalizedInverseProcessor == null) {
            var elementLocation = elementLocationMap.get(planningValue);
            if (elementLocation == null) {
                return null;
            }
            return elementLocation.entity();
        }
        return externalizedInverseProcessor.getInverseSingleton(planningValue);
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
        if (externalizedPreviousElementProcessor == null) {
            var elementLocation = getLocationInList(element);
            if (!(elementLocation instanceof LocationInList locationInList)) {
                return null;
            }
            var index = locationInList.index();
            if (index == 0) {
                return null;
            }
            return sourceVariableDescriptor.getValue(locationInList.entity()).get(index - 1);
        }
        return externalizedPreviousElementProcessor.getElement(element);
    }

    @Override
    public Object getNextElement(Object element) {
        if (externalizedNextElementProcessor == null) {
            var elementLocation = getLocationInList(element);
            if (!(elementLocation instanceof LocationInList locationInList)) {
                return null;
            }
            var list = sourceVariableDescriptor.getValue(locationInList.entity());
            var index = locationInList.index();
            if (index == list.size() - 1) {
                return null;
            }
            return list.get(index + 1);
        }
        return externalizedNextElementProcessor.getElement(element);
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
