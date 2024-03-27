package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;

record EntityOrderInfo(Object[] entities, Map<Object, Integer> entityToEntityIndex, int[] offsets) {

    public static <Node_> EntityOrderInfo of(Node_[] pickedValues, ListVariableStateSupply<?> listVariableStateSupply) {
        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        var entityToEntityIndex = new IdentityHashMap<Object, Integer>();
        for (int i = 1; i < pickedValues.length && pickedValues[i] != null; i++) {
            var value = pickedValues[i];
            var entity = listVariableStateSupply.getInverseSingleton(value);
            if (!listVariableDescriptor.getEntityDescriptor().isMovable(null, entity)) {
                throw new IllegalStateException("Impossible state: immovable entity (%s) picked through value (%s)."
                        .formatted(entity, value));
            }
            entityToEntityIndex.computeIfAbsent(entity, __ -> entityToEntityIndex.size());
        }
        var entities = new Object[entityToEntityIndex.size()];
        var offsets = new int[entities.length];
        for (var entityAndIndex : entityToEntityIndex.entrySet()) {
            entities[entityAndIndex.getValue()] = entityAndIndex.getKey();
        }
        for (int i = 1; i < offsets.length; i++) {
            offsets[i] = offsets[i - 1] + listVariableDescriptor.getListSize(entities[i - 1]);
        }
        return new EntityOrderInfo(entities, entityToEntityIndex, offsets);
    }

    public <Node_> EntityOrderInfo withNewNode(Node_ node, ListVariableStateSupply<?> listVariableStateSupply) {
        Object entity = listVariableStateSupply.getInverseSingleton(node);
        if (entityToEntityIndex.containsKey(entity)) {
            return this;
        } else {
            var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
            Object[] newEntities = Arrays.copyOf(entities, entities.length + 1);
            Map<Object, Integer> newEntityToEntityIndex = new IdentityHashMap<>(entityToEntityIndex);
            int[] newOffsets = Arrays.copyOf(offsets, offsets.length + 1);

            newEntities[entities.length] = entity;
            newEntityToEntityIndex.put(entity, entities.length);
            newOffsets[entities.length] =
                    offsets[entities.length - 1] + listVariableDescriptor.getListSize(entities[entities.length - 1]);
            return new EntityOrderInfo(newEntities, newEntityToEntityIndex, newOffsets);
        }
    }

    @SuppressWarnings("unchecked")
    public <Node_> Node_ successor(Node_ object, ListVariableStateSupply<?> listVariableStateSupply) {
        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        var elementLocation = (LocationInList) listVariableStateSupply.getLocationInList(object);
        var entity = elementLocation.entity();
        var indexInEntityList = elementLocation.index();
        var listVariable = listVariableDescriptor.getValue(entity);
        if (indexInEntityList == listVariable.size() - 1) {
            var nextEntityIndex = (entityToEntityIndex.get(entity) + 1) % entities.length;
            var nextEntity = entities[nextEntityIndex];
            var firstUnpinnedIndexInList = listVariableDescriptor.getFirstUnpinnedIndex(nextEntity);
            return (Node_) listVariableDescriptor.getElement(nextEntity, firstUnpinnedIndexInList);
        } else {
            return (Node_) listVariable.get(indexInEntityList + 1);
        }
    }

    @SuppressWarnings("unchecked")
    public <Node_> Node_ predecessor(Node_ object, ListVariableStateSupply<?> listVariableStateSupply) {
        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        var elementLocation = (LocationInList) listVariableStateSupply.getLocationInList(object);
        var entity = elementLocation.entity();
        var indexInEntityList = elementLocation.index();
        var firstUnpinnedIndexInList = listVariableDescriptor.getFirstUnpinnedIndex(entity);
        if (indexInEntityList == firstUnpinnedIndexInList) {
            // add entities.length to ensure modulo result is positive
            int previousEntityIndex = (entityToEntityIndex.get(entity) - 1 + entities.length) % entities.length;
            var listVariable = listVariableDescriptor.getValue(entities[previousEntityIndex]);
            return (Node_) listVariable.get(listVariable.size() - 1);
        } else {
            return (Node_) listVariableDescriptor.getElement(entity, indexInEntityList - 1);
        }
    }

    public <Node_> boolean between(Node_ start, Node_ middle, Node_ end, ListVariableStateSupply<?> listVariableStateSupply) {
        var startElementLocation = (LocationInList) listVariableStateSupply.getLocationInList(start);
        var middleElementLocation = (LocationInList) listVariableStateSupply.getLocationInList(middle);
        var endElementLocation = (LocationInList) listVariableStateSupply.getLocationInList(end);
        int startEntityIndex = entityToEntityIndex.get(startElementLocation.entity());
        int middleEntityIndex = entityToEntityIndex.get(middleElementLocation.entity());
        int endEntityIndex = entityToEntityIndex.get(endElementLocation.entity());

        int startIndex = startElementLocation.index() + offsets[startEntityIndex];
        int middleIndex = middleElementLocation.index() + offsets[middleEntityIndex];
        int endIndex = endElementLocation.index() + offsets[endEntityIndex];

        if (startIndex <= endIndex) {
            // test middleIndex in [startIndex, endIndex]
            return startIndex <= middleIndex && middleIndex <= endIndex;
        } else {
            // test middleIndex in [0, endIndex] or middleIndex in [startIndex, listSize)
            return middleIndex >= startIndex || middleIndex <= endIndex;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntityOrderInfo that
                && Arrays.equals(entities, that.entities)
                && Objects.equals(entityToEntityIndex, that.entityToEntityIndex)
                && Arrays.equals(offsets, that.offsets);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(entityToEntityIndex);
        result = 31 * result + Arrays.hashCode(entities);
        result = 31 * result + Arrays.hashCode(offsets);
        return result;
    }

    @Override
    public String toString() {
        return "EntityOrderInfo{" +
                "entities=" + Arrays.toString(entities) +
                ", entityToEntityIndex=" + entityToEntityIndex +
                ", offsets=" + Arrays.toString(offsets) +
                '}';
    }
}
