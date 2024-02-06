package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;

record EntityOrderInfo(Object[] entities, Map<Object, Integer> entityToEntityIndex, int[] offsets) {

    public static <Node_> EntityOrderInfo of(Node_[] pickedValues, SingletonInverseVariableSupply inverseVariableSupply,
            ListVariableDescriptor<?> listVariableDescriptor) {
        var entityToEntityIndex = new IdentityHashMap<Object, Integer>();
        for (int i = 1; i < pickedValues.length && pickedValues[i] != null; i++) {
            var value = pickedValues[i];
            var entity = inverseVariableSupply.getInverseSingleton(value);
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

    public <Node_> EntityOrderInfo withNewNode(Node_ node, ListVariableDescriptor<?> listVariableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply) {
        Object entity = inverseVariableSupply.getInverseSingleton(node);
        if (entityToEntityIndex.containsKey(entity)) {
            return this;
        } else {
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
    public <Node_> Node_ successor(Node_ object, ListVariableDescriptor<?> listVariableDescriptor,
            IndexVariableSupply indexVariableSupply, SingletonInverseVariableSupply inverseVariableSupply) {
        var entity = inverseVariableSupply.getInverseSingleton(object);
        var indexInEntityList = indexVariableSupply.getIndex(object);
        var listVariable = listVariableDescriptor.getListVariable(entity);
        if (indexInEntityList == listVariable.size() - 1) {
            var nextEntityIndex = (entityToEntityIndex.get(entity) + 1) % entities.length;
            var nextEntity = entities[nextEntityIndex];
            var firstUnpinnedIndexInList = listVariableDescriptor.getEntityDescriptor()
                    .extractFirstUnpinnedIndex(nextEntity);
            return (Node_) listVariableDescriptor.getListVariable(nextEntity)
                    .get(firstUnpinnedIndexInList);
        } else {
            return (Node_) listVariable.get(indexInEntityList + 1);
        }
    }

    @SuppressWarnings("unchecked")
    public <Node_> Node_ predecessor(Node_ object, ListVariableDescriptor<?> listVariableDescriptor,
            IndexVariableSupply indexVariableSupply, SingletonInverseVariableSupply inverseVariableSupply) {
        var entity = inverseVariableSupply.getInverseSingleton(object);
        var indexInEntityList = indexVariableSupply.getIndex(object);
        var firstUnpinnedIndexInList = listVariableDescriptor.getEntityDescriptor()
                .extractFirstUnpinnedIndex(entity);
        var listVariable = listVariableDescriptor.getListVariable(entity);
        if (indexInEntityList == firstUnpinnedIndexInList) {
            // add entities.length to ensure modulo result is positive
            int previousEntityIndex = (entityToEntityIndex.get(entity) - 1 + entities.length) % entities.length;
            listVariable = listVariableDescriptor.getListVariable(entities[previousEntityIndex]);
            return (Node_) listVariable.get(listVariable.size() - 1);
        } else {
            return (Node_) listVariable.get(indexInEntityList - 1);
        }
    }

    public <Node_> boolean between(Node_ start, Node_ middle, Node_ end, IndexVariableSupply indexVariableSupply,
            SingletonInverseVariableSupply inverseVariableSupply) {
        int startEntityIndex = entityToEntityIndex.get(inverseVariableSupply.getInverseSingleton(start));
        int middleEntityIndex = entityToEntityIndex.get(inverseVariableSupply.getInverseSingleton(middle));
        int endEntityIndex = entityToEntityIndex.get(inverseVariableSupply.getInverseSingleton(end));

        int startIndex = indexVariableSupply.getIndex(start) + offsets[startEntityIndex];
        int middleIndex = indexVariableSupply.getIndex(middle) + offsets[middleEntityIndex];
        int endIndex = indexVariableSupply.getIndex(end) + offsets[endEntityIndex];

        if (startIndex <= endIndex) {
            // test middleIndex in [startIndex, endIndex]
            return startIndex <= middleIndex && middleIndex <= endIndex;
        } else {
            // test middleIndex in [0, endIndex] or middleIndex in [startIndex, listSize)
            return middleIndex >= startIndex || middleIndex <= endIndex;
        }
    }
}
