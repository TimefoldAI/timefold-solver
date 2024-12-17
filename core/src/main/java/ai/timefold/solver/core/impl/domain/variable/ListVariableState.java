package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;
import java.util.Map;

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
    private ExternalizedListInverseVariableProcessor<Solution_> externalizedInverseProcessor = null;
    private ExternalizedNextPrevElementVariableProcessor<Solution_> externalizedPreviousElementProcessor = null;
    private ExternalizedNextPrevElementVariableProcessor<Solution_> externalizedNextElementProcessor = null;

    private boolean requiresLocationMap = true;
    private InnerScoreDirector<Solution_, ?> scoreDirector;
    private int unassignedCount = 0;
    private Map<Object, MutableLocationInList> elementLocationMap;

    public ListVariableState(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    public void linkShadowVariable(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedIndexProcessor = new ExternalizedIndexVariableProcessor<>(shadowVariableDescriptor);
    }

    public void linkShadowVariable(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedInverseProcessor =
                new ExternalizedListInverseVariableProcessor<>(shadowVariableDescriptor, sourceVariableDescriptor);
    }

    public void linkShadowVariable(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedPreviousElementProcessor =
                ExternalizedNextPrevElementVariableProcessor.ofPrevious(shadowVariableDescriptor);
    }

    public void linkShadowVariable(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.externalizedNextElementProcessor = ExternalizedNextPrevElementVariableProcessor.ofNext(shadowVariableDescriptor);
    }

    public void initialize(InnerScoreDirector<Solution_, ?> scoreDirector, int initialUnassignedCount) {
        this.scoreDirector = scoreDirector;
        this.unassignedCount = initialUnassignedCount;

        this.requiresLocationMap = externalizedIndexProcessor == null || externalizedInverseProcessor == null
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
            var oldLocation = elementLocationMap.put(element, new MutableLocationInList(entity, index));
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
            var oldIndex = oldElementLocation.getIndex();
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
        var difference = processElementLocation(entity, element, index);
        if (difference.indexChanged && externalizedIndexProcessor != null) {
            externalizedIndexProcessor.changeElement(scoreDirector, element, index);
        }
        if (difference.entityChanged && externalizedInverseProcessor != null) {
            externalizedInverseProcessor.changeElement(scoreDirector, entity, element);
        }
        // Next and previous still might have changed, even if the index and entity did not.
        // Those are based on what happened elsewhere in the list.
        if (externalizedPreviousElementProcessor != null) {
            externalizedPreviousElementProcessor.setElement(scoreDirector, elements, element, index);
        }
        if (externalizedNextElementProcessor != null) {
            externalizedNextElementProcessor.setElement(scoreDirector, elements, element, index);
        }
        return difference.anythingChanged;
    }

    private ChangeType processElementLocation(Object entity, Object element, int index) {
        if (requiresLocationMap) { // Update the location and figure out if it is different from previous.
            var oldLocation = elementLocationMap.get(element);
            if (oldLocation == null) {
                elementLocationMap.put(element, new MutableLocationInList(entity, index));
                unassignedCount--;
                return ChangeType.BOTH;
            }
            var changeType = compareLocations(entity, oldLocation.getEntity(), index, oldLocation.getIndex());
            if (changeType.anythingChanged) { // Replace the map value in-place, to avoid a put() on the hot path.
                if (changeType.entityChanged) {
                    oldLocation.setEntity(entity);
                }
                if (changeType.indexChanged) {
                    oldLocation.setIndex(index);
                }
            }
            return changeType;
        } else { // Read the location and figure out if it is different from previous.
            var oldEntity = getInverseSingleton(element);
            if (oldEntity == null) {
                unassignedCount--;
                return ChangeType.BOTH;
            }
            var oldIndex = getIndex(element);
            if (oldIndex == null) { // Technically impossible, but we handle it anyway.
                return ChangeType.BOTH;
            }
            return compareLocations(entity, oldEntity, index, oldIndex);
        }
    }

    private static ChangeType compareLocations(Object entity, Object otherEntity, int index, int otherIndex) {
        if (entity != otherEntity) {
            return ChangeType.BOTH; // Entity changed, so index changed too.
        } else if (index != otherIndex) {
            return ChangeType.INDEX;
        } else {
            return ChangeType.NEITHER;
        }
    }

    public ElementLocation getLocationInList(Object planningValue) {
        if (requiresLocationMap) {
            var mutableLocationInList = elementLocationMap.get(planningValue);
            if (mutableLocationInList == null) {
                return ElementLocation.unassigned();
            }
            return mutableLocationInList.getLocationInList();
        } else { // At this point, both inverse and index are externalized.
            var inverse = externalizedInverseProcessor.getInverseSingleton(planningValue);
            if (inverse == null) {
                return ElementLocation.unassigned();
            }
            return ElementLocation.of(inverse, externalizedIndexProcessor.getIndex(planningValue));
        }
    }

    public Integer getIndex(Object planningValue) {
        if (externalizedIndexProcessor == null) {
            var elementLocation = elementLocationMap.get(planningValue);
            if (elementLocation == null) {
                return null;
            }
            return elementLocation.getIndex();
        }
        return externalizedIndexProcessor.getIndex(planningValue);
    }

    public Object getInverseSingleton(Object planningValue) {
        if (externalizedInverseProcessor == null) {
            var elementLocation = elementLocationMap.get(planningValue);
            if (elementLocation == null) {
                return null;
            }
            return elementLocation.getEntity();
        }
        return externalizedInverseProcessor.getInverseSingleton(planningValue);
    }

    public Object getPreviousElement(Object element) {
        if (externalizedPreviousElementProcessor == null) {
            var mutableLocationInList = elementLocationMap.get(element);
            if (mutableLocationInList == null) {
                return null;
            }
            var index = mutableLocationInList.getIndex();
            if (index == 0) {
                return null;
            }
            return sourceVariableDescriptor.getValue(mutableLocationInList.getEntity())
                    .get(index - 1);
        }
        return externalizedPreviousElementProcessor.getElement(element);
    }

    public Object getNextElement(Object element) {
        if (externalizedNextElementProcessor == null) {
            var mutableLocationInList = elementLocationMap.get(element);
            if (mutableLocationInList == null) {
                return null;
            }
            var list = sourceVariableDescriptor.getValue(mutableLocationInList.getEntity());
            var index = mutableLocationInList.getIndex();
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

    private enum ChangeType {

        BOTH(true, true),
        INDEX(false, true),
        NEITHER(false, false);

        final boolean anythingChanged;
        final boolean entityChanged;
        final boolean indexChanged;

        ChangeType(boolean entityChanged, boolean indexChanged) {
            this.anythingChanged = entityChanged || indexChanged;
            this.entityChanged = entityChanged;
            this.indexChanged = indexChanged;
        }

    }

    /**
     * This class is used to avoid creating a new {@link LocationInList} object every time we need to return a location.
     * The actual value is held in a map and can be updated without doing a put() operation, which is more efficient.
     * The {@link LocationInList} object is only created when it is actually requested,
     * and stored until the next time the mutable state is updated and therefore the cache invalidated.
     */
    private static final class MutableLocationInList {

        private Object entity;
        private int index;
        private LocationInList locationInList;

        public MutableLocationInList(Object entity, int index) {
            this.entity = entity;
            this.index = index;
        }

        public Object getEntity() {
            return entity;
        }

        public void setEntity(Object entity) {
            this.entity = entity;
            this.locationInList = null;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
            this.locationInList = null;
        }

        public LocationInList getLocationInList() {
            if (locationInList == null) {
                locationInList = ElementLocation.of(entity, index);
            }
            return locationInList;
        }

    }

}
