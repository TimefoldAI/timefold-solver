package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.preview.api.domain.metamodel.LocationInList;

final class ListVariableState<Solution_> {

    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    private ExternalizedIndexVariableProcessor<Solution_> externalizedIndexProcessor = null;
    private ExternalizedSingletonListInverseVariableProcessor<Solution_> externalizedInverseProcessor = null;
    private AbstractExternalizedNextPrevElementVariableProcessor<Solution_> externalizedPreviousElementProcessor = null;
    private AbstractExternalizedNextPrevElementVariableProcessor<Solution_> externalizedNextElementProcessor = null;

    private boolean canUseExternalizedLocation = false;
    private boolean requiresLocationMap = true;
    private InnerScoreDirector<Solution_, ?> scoreDirector;
    private int unassignedCount = 0;
    private Map<Object, LocationInList> elementLocationMap;

    public ListVariableState(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    public void linkDescriptor(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedIndexProcessor = new ExternalizedIndexVariableProcessor<>(shadowVariableDescriptor);
    }

    public void linkDescriptor(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedInverseProcessor =
                new ExternalizedSingletonListInverseVariableProcessor<>(shadowVariableDescriptor, sourceVariableDescriptor);
    }

    public void linkDescriptor(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedPreviousElementProcessor =
                new ExternalizedPreviousElementVariableProcessor<>(shadowVariableDescriptor);
    }

    public void linkDescriptor(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedNextElementProcessor = new ExternalizedNextElementVariableProcessor<>(shadowVariableDescriptor);
    }

    public void initialize(InnerScoreDirector<Solution_, ?> scoreDirector, int initialUnassignedCount) {
        this.scoreDirector = scoreDirector;
        this.unassignedCount = initialUnassignedCount;

        this.canUseExternalizedLocation = externalizedIndexProcessor != null && externalizedInverseProcessor != null;
        this.requiresLocationMap = !canUseExternalizedLocation
                || externalizedPreviousElementProcessor == null || externalizedNextElementProcessor == null;
        if (requiresLocationMap) {
            if (elementLocationMap == null) {
                elementLocationMap = CollectionUtils.newIdentityHashMap(unassignedCount);
            } else {
                elementLocationMap.clear();
            }
        } else {
            elementLocationMap = null;
        }
    }

    public void addElement(Object entity, List<Object> elements, Object element, int index) {
        if (requiresLocationMap) {
            var location = ElementLocation.of(entity, index);
            var oldLocation = elementLocationMap.put(element, location);
            if (oldLocation != null) {
                throw new IllegalStateException(
                        "The supply for list variable (%s) is corrupted, because the element (%s) at index (%d) already exists (%s)."
                                .formatted(sourceVariableDescriptor, element, index, oldLocation));
            }
        }
        if (externalizedIndexProcessor != null) {
            externalizedIndexProcessor.addElement(scoreDirector, element, index);
        }
        if (externalizedInverseProcessor != null) {
            externalizedInverseProcessor.addElement(scoreDirector, entity, element);
        }
        if (externalizedPreviousElementProcessor != null) {
            externalizedPreviousElementProcessor.setElement(scoreDirector, elements, element, index);
        }
        if (externalizedNextElementProcessor != null) {
            externalizedNextElementProcessor.setElement(scoreDirector, elements, element, index);
        }
        unassignedCount--;
    }

    public void removeElement(Object entity, Object element, int index) {
        if (requiresLocationMap) {
            var oldElementLocation = elementLocationMap.remove(element);
            if (oldElementLocation == null) {
                throw new IllegalStateException(
                        "The supply for list variable (%s) is corrupted, because the element (%s) at index (%d) was already unassigned (%s)."
                                .formatted(sourceVariableDescriptor, element, index, oldElementLocation));
            }
            var oldIndex = oldElementLocation.index();
            if (oldIndex != index) {
                throw new IllegalStateException(
                        "The supply for list variable (%s) is corrupted, because the element (%s) at index (%d) had an old index (%d) which is not the current index (%d)."
                                .formatted(sourceVariableDescriptor, element, index, oldIndex, index));
            }
        }
        if (externalizedIndexProcessor != null) {
            externalizedIndexProcessor.removeElement(scoreDirector, element);
        }
        if (externalizedInverseProcessor != null) {
            externalizedInverseProcessor.removeElement(scoreDirector, entity, element);
        }
        if (externalizedPreviousElementProcessor != null) {
            externalizedPreviousElementProcessor.unsetElement(scoreDirector, element);
        }
        if (externalizedNextElementProcessor != null) {
            externalizedNextElementProcessor.unsetElement(scoreDirector, element);
        }
        unassignedCount++;
    }

    public void unassignElement(Object element) {
        if (requiresLocationMap) {
            var oldLocation = elementLocationMap.remove(element);
            if (oldLocation == null) {
                throw new IllegalStateException(
                        "The supply for list variable (%s) is corrupted, because the element (%s) did not exist before unassigning."
                                .formatted(sourceVariableDescriptor, element));
            }
        }
        if (externalizedIndexProcessor != null) {
            externalizedIndexProcessor.unassignElement(scoreDirector, element);
        }
        if (externalizedInverseProcessor != null) {
            externalizedInverseProcessor.unassignElement(scoreDirector, element);
        }
        if (externalizedPreviousElementProcessor != null) {
            externalizedPreviousElementProcessor.unsetElement(scoreDirector, element);
        }
        if (externalizedNextElementProcessor != null) {
            externalizedNextElementProcessor.unsetElement(scoreDirector, element);
        }
        unassignedCount++;
    }

    public boolean changeElement(Object entity, List<Object> elements, int index) {
        var element = elements.get(index);
        var locationsDiffer = processElementLocation(entity, element, index);
        if (externalizedIndexProcessor != null) {
            externalizedIndexProcessor.changeElement(scoreDirector, element, index);
        }
        if (externalizedInverseProcessor != null) {
            externalizedInverseProcessor.changeElement(scoreDirector, entity, element);
        }
        if (externalizedPreviousElementProcessor != null) {
            externalizedPreviousElementProcessor.setElement(scoreDirector, elements, element, index);
        }
        if (externalizedNextElementProcessor != null) {
            externalizedNextElementProcessor.setElement(scoreDirector, elements, element, index);
        }
        return locationsDiffer;
    }

    private boolean processElementLocation(Object entity, Object element, int index) {
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
            return previousEntity != entity || !equalsIntegerAndInt(getIndex(element), index);
        }
    }

    private static boolean equalsIntegerAndInt(Integer integer, int i) {
        return integer != null && integer == i;
    }

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

    public int getUnassignedCount() {
        return unassignedCount;
    }

    void close() {
        elementLocationMap = null;
    }

}
