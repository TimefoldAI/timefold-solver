package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;

final class KOptListMoveIterator<Solution_, Node_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final Random workingRandom;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final SingletonInverseVariableSupply inverseVariableSupply;
    private final IndexVariableSupply indexVariableSupply;
    private final EntityIndependentValueSelector<Node_> originSelector;
    private final EntityIndependentValueSelector<Node_> valueSelector;
    private final int minK;
    private final int[] pickedKDistribution;
    private final int pickedKDistributionSum;
    private final int maxCyclesPatchedInInfeasibleMove;

    public KOptListMoveIterator(Random workingRandom,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply,
            IndexVariableSupply indexVariableSupply,
            EntityIndependentValueSelector<Node_> originSelector,
            EntityIndependentValueSelector<Node_> valueSelector,
            int minK,
            int maxK,
            int[] pickedKDistribution) {
        this.workingRandom = workingRandom;
        this.listVariableDescriptor = listVariableDescriptor;
        this.inverseVariableSupply = inverseVariableSupply;
        this.indexVariableSupply = indexVariableSupply;
        this.originSelector = originSelector;
        this.valueSelector = valueSelector;
        this.minK = minK;
        this.pickedKDistribution = pickedKDistribution;
        int tmpPickedKDistributionSum = 0;
        for (int relativeDistributionAmount : pickedKDistribution) {
            tmpPickedKDistributionSum += relativeDistributionAmount;
        }
        this.pickedKDistributionSum = tmpPickedKDistributionSum;
        this.maxCyclesPatchedInInfeasibleMove = maxK;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        int locationInDistribution = workingRandom.nextInt(pickedKDistributionSum);
        int indexInDistribution = 0;
        while (locationInDistribution >= pickedKDistribution[indexInDistribution]) {
            locationInDistribution -= pickedKDistribution[indexInDistribution];
            indexInDistribution++;
        }
        int k = minK + indexInDistribution;
        if (k == 2) {
            return pickTwoOptMove();
        }
        KOptDescriptor<Node_> descriptor = pickKOptMove(k);
        if (descriptor == null) {
            // Was unable to find a K-Opt move
            return new NoChangeMove<>();
        }
        return descriptor.getKOptListMove(listVariableDescriptor, indexVariableSupply, inverseVariableSupply);
    }

    private TwoOptListMove<Solution_> pickTwoOptMove() {
        @SuppressWarnings("unchecked")
        Iterator<Node_> originIterator = (Iterator<Node_>) originSelector.iterator();

        @SuppressWarnings("unchecked")
        Iterator<Node_> valueIterator = (Iterator<Node_>) valueSelector.iterator();

        Object firstValue = originIterator.next();
        Object secondValue = valueIterator.next();

        Object firstEntity = inverseVariableSupply.getInverseSingleton(firstValue);
        Object secondEntity = inverseVariableSupply.getInverseSingleton(secondValue);

        return new TwoOptListMove<>(listVariableDescriptor, firstEntity, secondEntity,
                indexVariableSupply.getIndex(firstValue), indexVariableSupply.getIndex(secondValue));
    }

    @SuppressWarnings("unchecked")
    private Iterator<Node_> getValuesOnSelectedEntitiesIterator(Node_[] pickedValues) {
        var entityOrderInfo = EntityOrderInfo.of(pickedValues, inverseVariableSupply, listVariableDescriptor);
        return (Iterator<Node_>) workingRandom.ints(0, entityOrderInfo.entities().length)
                .mapToObj(index -> {
                    var entity = entityOrderInfo.entities()[index];
                    var listVariable = listVariableDescriptor.getListVariable(entity);
                    var firstUnpinnedIndex = listVariableDescriptor.getEntityDescriptor()
                            .extractFirstUnpinnedIndex(entity);
                    return listVariable
                            .get(workingRandom.nextInt(listVariable.size() - firstUnpinnedIndex) + firstUnpinnedIndex);
                })
                .iterator();
    }

    @SuppressWarnings("unchecked")
    private KOptDescriptor<Node_> pickKOptMove(int k) {
        // The code in the paper used 1-index arrays
        Node_[] pickedValues = (Node_[]) new Object[2 * k + 1];
        Iterator<Node_> originIterator = (Iterator<Node_>) originSelector.iterator();

        pickedValues[1] = originIterator.next();
        int remainingAttempts = 20;
        while (remainingAttempts > 0
                && getEffectiveListSize(listVariableDescriptor,
                        inverseVariableSupply.getInverseSingleton(pickedValues[1])) < 2) {
            pickedValues[1] = originIterator.next();
            remainingAttempts--;
        }

        if (remainingAttempts == 0) {
            // could not find a value in a list with more than 1 element
            return null;
        }

        EntityOrderInfo entityOrderInfo = EntityOrderInfo.of(pickedValues, inverseVariableSupply, listVariableDescriptor);
        pickedValues[2] = workingRandom.nextBoolean() ? getNodeSuccessor(entityOrderInfo, pickedValues[1])
                : getNodePredecessor(entityOrderInfo, pickedValues[1]);

        if (isNodeEndpointOfList(pickedValues[1]) || isNodeEndpointOfList(pickedValues[2])) {
            return pickKOptMoveRec(getValuesOnSelectedEntitiesIterator(pickedValues), entityOrderInfo, pickedValues, 2, k,
                    false);
        } else {
            return pickKOptMoveRec((Iterator<Node_>) valueSelector.iterator(), entityOrderInfo, pickedValues, 2, k, true);
        }
    }

    private static <Solution_> int getEffectiveListSize(ListVariableDescriptor<Solution_> listVariableDescriptor,
            Object entity) {
        var listSize = listVariableDescriptor.getListSize(entity);
        var firstUnpinnedIndex = listVariableDescriptor.getEntityDescriptor()
                .extractFirstUnpinnedIndex(entity);
        return listSize - firstUnpinnedIndex;
    }

    private KOptDescriptor<Node_> pickKOptMoveRec(Iterator<Node_> valueIterator,
            EntityOrderInfo entityOrderInfo,
            Node_[] pickedValues,
            int pickedSoFar,
            int k,
            boolean canSelectNewEntities) {
        Node_ previousRemovedEdgeEndpoint = pickedValues[2 * pickedSoFar - 2];
        Node_ nextRemovedEdgePoint, nextRemovedEdgeOppositePoint;

        int remainingAttempts = (k - pickedSoFar + 3) * 2;
        while (remainingAttempts > 0) {
            nextRemovedEdgePoint = valueIterator.next();
            EntityOrderInfo newEntityOrderInfo =
                    entityOrderInfo.withNewNode(nextRemovedEdgePoint, listVariableDescriptor, inverseVariableSupply);
            while (nextRemovedEdgePoint == getNodePredecessor(newEntityOrderInfo, previousRemovedEdgeEndpoint) ||
                    nextRemovedEdgePoint == getNodeSuccessor(newEntityOrderInfo, previousRemovedEdgeEndpoint) ||
                    isEdgeAlreadyAdded(pickedValues, previousRemovedEdgeEndpoint, nextRemovedEdgePoint, pickedSoFar - 2) ||
                    (isEdgeAlreadyDeleted(pickedValues, nextRemovedEdgePoint,
                            getNodePredecessor(newEntityOrderInfo, nextRemovedEdgePoint),
                            pickedSoFar - 2)
                            && isEdgeAlreadyDeleted(pickedValues, nextRemovedEdgePoint,
                                    getNodeSuccessor(newEntityOrderInfo, nextRemovedEdgePoint),
                                    pickedSoFar - 2))) {
                if (remainingAttempts == 0) {
                    return null;
                }
                nextRemovedEdgePoint = valueIterator.next();
                newEntityOrderInfo =
                        entityOrderInfo.withNewNode(nextRemovedEdgePoint, listVariableDescriptor, inverseVariableSupply);
                remainingAttempts--;
            }
            remainingAttempts--;

            pickedValues[2 * pickedSoFar - 1] = nextRemovedEdgePoint;
            if (isEdgeAlreadyDeleted(pickedValues, nextRemovedEdgePoint,
                    getNodePredecessor(newEntityOrderInfo, nextRemovedEdgePoint),
                    pickedSoFar - 2)) {
                nextRemovedEdgeOppositePoint = getNodeSuccessor(newEntityOrderInfo, nextRemovedEdgePoint);
            } else if (isEdgeAlreadyDeleted(pickedValues, nextRemovedEdgePoint,
                    getNodeSuccessor(newEntityOrderInfo, nextRemovedEdgePoint),
                    pickedSoFar - 2)) {
                nextRemovedEdgeOppositePoint = getNodePredecessor(newEntityOrderInfo, nextRemovedEdgePoint);
            } else {
                nextRemovedEdgeOppositePoint =
                        workingRandom.nextBoolean() ? getNodeSuccessor(newEntityOrderInfo, nextRemovedEdgePoint)
                                : getNodePredecessor(newEntityOrderInfo, nextRemovedEdgePoint);
            }
            pickedValues[2 * pickedSoFar] = nextRemovedEdgeOppositePoint;

            if (canSelectNewEntities && isNodeEndpointOfList(nextRemovedEdgePoint)
                    || isNodeEndpointOfList(nextRemovedEdgeOppositePoint)) {
                valueIterator = getValuesOnSelectedEntitiesIterator(pickedValues);
                canSelectNewEntities = false;
            }

            if (pickedSoFar < k) {
                KOptDescriptor<Node_> descriptor = pickKOptMoveRec(valueIterator, newEntityOrderInfo, pickedValues,
                        pickedSoFar + 1, k, canSelectNewEntities);
                if (descriptor != null && descriptor.isFeasible(minK, maxCyclesPatchedInInfeasibleMove)) {
                    return descriptor;
                }
            } else {
                KOptDescriptor<Node_> descriptor = new KOptDescriptor<>(pickedValues,
                        KOptUtils.getMultiEntitySuccessorFunction(pickedValues,
                                listVariableDescriptor,
                                inverseVariableSupply,
                                indexVariableSupply),
                        KOptUtils.getMultiEntityBetweenPredicate(pickedValues,
                                listVariableDescriptor,
                                inverseVariableSupply,
                                indexVariableSupply));
                if (descriptor.isFeasible(minK, maxCyclesPatchedInInfeasibleMove)) {
                    return descriptor;
                } else {
                    descriptor = patchCycles(
                            descriptor,
                            newEntityOrderInfo, pickedValues,
                            pickedSoFar);
                    if (descriptor.isFeasible(minK, maxCyclesPatchedInInfeasibleMove)) {
                        return descriptor;
                    }
                }
            }
        }
        return null;
    }

    KOptDescriptor<Node_> patchCycles(KOptDescriptor<Node_> descriptor, EntityOrderInfo entityOrderInfo,
            Node_[] oldRemovedEdges, int k) {
        Node_ s1, s2;
        int[] removedEdgeIndexToTourOrder = descriptor.removedEdgeIndexToTourOrder();
        Iterator<Node_> valueIterator = getValuesOnSelectedEntitiesIterator(oldRemovedEdges);
        KOptCycle cycleInfo = KOptUtils.getCyclesForPermutation(descriptor);
        int cycleCount = cycleInfo.cycleCount();
        int[] cycle = cycleInfo.indexToCycleIdentifier();

        // If cycleCount != 1,
        // we are changing an infeasible k-opt move that results in cycleCount cycles
        // into a (k+cycleCount) move.
        // If the k+cycleCount > maxK, we should ignore generating the move
        // Note: maxCyclesPatchedInInfeasibleMove = maxK
        if (cycleCount == 1 || k + cycleCount > maxCyclesPatchedInInfeasibleMove) {
            return descriptor;
        }
        int currentCycle =
                getShortestCycleIdentifier(entityOrderInfo, oldRemovedEdges, cycle, removedEdgeIndexToTourOrder, cycleCount, k);

        for (int i = 0; i < k; i++) {
            if (cycle[removedEdgeIndexToTourOrder[2 * i]] == currentCycle) {
                Node_ sStart = oldRemovedEdges[removedEdgeIndexToTourOrder[2 * i]];
                Node_ sStop = oldRemovedEdges[removedEdgeIndexToTourOrder[2 * i + 1]];
                for (s1 = sStart; s1 != sStop; s1 = s2) {
                    Node_[] removedEdges = Arrays.copyOf(oldRemovedEdges, oldRemovedEdges.length + 2);

                    removedEdges[2 * k + 1] = s1;
                    s2 = getNodeSuccessor(entityOrderInfo, s1);
                    removedEdges[2 * k + 2] = s2;
                    int[] addedEdgeToOtherEndpoint =
                            Arrays.copyOf(KOptDescriptor.computeInEdgesForSequentialMove(oldRemovedEdges),
                                    removedEdges.length + 2 + (2 * cycleCount));
                    for (int newEdge = removedEdges.length; newEdge < addedEdgeToOtherEndpoint.length - 2; newEdge++) {
                        addedEdgeToOtherEndpoint[newEdge] = newEdge + 2;
                    }
                    addedEdgeToOtherEndpoint[addedEdgeToOtherEndpoint.length - 1] = addedEdgeToOtherEndpoint.length - 3;
                    addedEdgeToOtherEndpoint[addedEdgeToOtherEndpoint.length - 2] = addedEdgeToOtherEndpoint.length - 4;
                    KOptDescriptor<Node_> newMove = patchCyclesRec(valueIterator, descriptor, entityOrderInfo, removedEdges,
                            addedEdgeToOtherEndpoint, cycle, currentCycle,
                            k, 2, cycleCount);
                    if (newMove.isFeasible(minK, maxCyclesPatchedInInfeasibleMove)) {
                        return newMove;
                    }
                }
            }
        }
        return descriptor;
    }

    KOptDescriptor<Node_> patchCyclesRec(Iterator<Node_> valueIterator,
            KOptDescriptor<Node_> originalMove, EntityOrderInfo entityOrderInfo,
            Node_[] oldRemovedEdges, int[] addedEdgeToOtherEndpoint, int[] cycle, int currentCycle,
            int k, int patchedCycleCount, int cycleCount) {
        Node_ s1, s2, s3, s4;
        int NewCycle, i;
        Integer[] cycleSaved = new Integer[1 + 2 * k];
        Node_[] removedEdges = Arrays.copyOf(oldRemovedEdges, oldRemovedEdges.length + 2);

        s1 = removedEdges[2 * k + 1];
        s2 = removedEdges[i = 2 * (k + patchedCycleCount) - 2];
        addedEdgeToOtherEndpoint[addedEdgeToOtherEndpoint[i] = i + 1] = i;

        for (i = 1; i <= 2 * k; i++) {
            cycleSaved[i] = cycle[i];
        }
        s3 = valueIterator.next();
        int remainingAttempts = cycleCount * 2;
        while (s3 == getNodePredecessor(entityOrderInfo, s2) || s3 == getNodeSuccessor(entityOrderInfo, s2)
                || ((NewCycle = findCycleIdentifierForNode(entityOrderInfo, s3, removedEdges,
                        originalMove.removedEdgeIndexToTourOrder(),
                        cycle)) == currentCycle)
                ||
                (isEdgeAlreadyDeleted(removedEdges, s3, getNodePredecessor(entityOrderInfo, s3), k)
                        && isEdgeAlreadyDeleted(removedEdges, s3, getNodeSuccessor(entityOrderInfo, s3), k))) {
            if (remainingAttempts == 0) {
                return originalMove;
            }
            s3 = valueIterator.next();
            remainingAttempts--;
        }

        removedEdges[2 * (k + patchedCycleCount) - 1] = s3;
        if (isEdgeAlreadyDeleted(removedEdges, s3, getNodePredecessor(entityOrderInfo, s3), k)) {
            s4 = getNodeSuccessor(entityOrderInfo, s3);
        } else if (isEdgeAlreadyDeleted(removedEdges, s3, getNodeSuccessor(entityOrderInfo, s3), k)) {
            s4 = getNodePredecessor(entityOrderInfo, s3);
        } else {
            s4 = workingRandom.nextBoolean() ? getNodeSuccessor(entityOrderInfo, s3) : getNodePredecessor(entityOrderInfo, s3);
        }
        removedEdges[2 * (k + patchedCycleCount)] = s4;
        if (cycleCount > 2) {
            for (i = 1; i <= 2 * k; i++) {
                if (cycle[i] == NewCycle) {
                    cycle[i] = currentCycle;
                }
            }
            KOptDescriptor<Node_> recursiveCall =
                    patchCyclesRec(valueIterator, originalMove, entityOrderInfo, removedEdges, addedEdgeToOtherEndpoint, cycle,
                            currentCycle,
                            k, patchedCycleCount + 1, cycleCount - 1);
            if (recursiveCall.isFeasible(minK, maxCyclesPatchedInInfeasibleMove)) {
                return recursiveCall;
            }
            for (i = 1; i <= 2 * k; i++) {
                cycle[i] = cycleSaved[i];
            }
        } else if (s4 != s1) {
            addedEdgeToOtherEndpoint[addedEdgeToOtherEndpoint[2 * k + 1] = 2 * (k + patchedCycleCount)] =
                    2 * k + 1;
            return new KOptDescriptor<>(removedEdges, addedEdgeToOtherEndpoint,
                    KOptUtils.getMultiEntitySuccessorFunction(removedEdges,
                            listVariableDescriptor,
                            inverseVariableSupply,
                            indexVariableSupply),
                    KOptUtils.getMultiEntityBetweenPredicate(removedEdges,
                            listVariableDescriptor,
                            inverseVariableSupply,
                            indexVariableSupply));
        }
        return originalMove;
    }

    int findCycleIdentifierForNode(EntityOrderInfo entityOrderInfo, Node_ value, Node_[] pickedValues, int[] permutation,
            int[] indexToCycle) {
        for (int i = 1; i < pickedValues.length; i++) {
            if (isMiddleNodeBetween(entityOrderInfo, pickedValues[permutation[i - 1]], value, pickedValues[permutation[i]])) {
                return indexToCycle[permutation[i]];
            }
        }
        throw new IllegalStateException("Cannot find cycle the " + value + " belongs to");
    }

    int getShortestCycleIdentifier(EntityOrderInfo entityOrderInfo, Object[] removeEdgeEndpoints, int[] endpointIndexToCycle,
            int[] removeEdgeEndpointIndexToTourOrder, int cycleCount, int k) {
        int i;
        int minCycleIdentifier = 0;
        int minSize = Integer.MAX_VALUE;
        int[] size = new int[cycleCount + 1];

        for (i = 1; i <= cycleCount; i++) {
            size[i] = 0;
        }
        removeEdgeEndpointIndexToTourOrder[0] = removeEdgeEndpointIndexToTourOrder[2 * k];
        for (i = 0; i < 2 * k; i += 2) {
            size[endpointIndexToCycle[removeEdgeEndpointIndexToTourOrder[i]]] +=
                    getSegmentSize(entityOrderInfo, removeEdgeEndpoints[removeEdgeEndpointIndexToTourOrder[i]],
                            removeEdgeEndpoints[removeEdgeEndpointIndexToTourOrder[i + 1]]);
        }
        for (i = 1; i <= cycleCount; i++) {
            if (size[i] < minSize) {
                minSize = size[i];
                minCycleIdentifier = i - 1;
            }
        }
        return minCycleIdentifier;
    }

    private int getSegmentSize(EntityOrderInfo entityOrderInfo, Object from, Object to) {
        var entityToEntityIndex = entityOrderInfo.entityToEntityIndex();
        int startEntityIndex = entityToEntityIndex.get(inverseVariableSupply.getInverseSingleton(from));
        int endEntityIndex = entityToEntityIndex.get(inverseVariableSupply.getInverseSingleton(to));
        var offsets = entityOrderInfo.offsets();
        int startIndex = offsets[startEntityIndex] + indexVariableSupply.getIndex(from);
        int endIndex = offsets[endEntityIndex] + indexVariableSupply.getIndex(to);

        if (startIndex <= endIndex) {
            return endIndex - startIndex;
        } else {
            var entities = entityOrderInfo.entities();
            int totalRouteSize =
                    offsets[offsets.length - 1] + listVariableDescriptor.getListSize(entities[entities.length - 1]);
            return totalRouteSize - startIndex + endIndex;
        }
    }

    private boolean isEdgeAlreadyAdded(Object[] pickedValues, Object ta, Object tb, int k) {
        int i = 2 * k;
        while ((i -= 2) > 0) {
            if ((ta == pickedValues[i] && tb == pickedValues[i + 1]) ||
                    (ta == pickedValues[i + 1] && tb == pickedValues[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isEdgeAlreadyDeleted(Object[] pickedValues, Object ta, Object tb, int k) {
        int i = 2 * k + 2;
        while ((i -= 2) > 0) {
            if ((ta == pickedValues[i - 1] && tb == pickedValues[i]) ||
                    (ta == pickedValues[i] && tb == pickedValues[i - 1])) {
                return true;
            }
        }
        return false;
    }

    private boolean isNodeEndpointOfList(Object node) {
        int index = indexVariableSupply.getIndex(node);
        int firstUnpinnedIndex = listVariableDescriptor.getEntityDescriptor()
                .extractFirstUnpinnedIndex(inverseVariableSupply.getInverseSingleton(node));
        if (index == firstUnpinnedIndex) {
            return true;
        }
        int size = listVariableDescriptor.getListSize(inverseVariableSupply.getInverseSingleton(node));
        return index == size - 1;
    }

    private Node_ getNodeSuccessor(EntityOrderInfo entityOrderInfo, Node_ node) {
        return entityOrderInfo.successor(node, listVariableDescriptor, indexVariableSupply, inverseVariableSupply);
    }

    private Node_ getNodePredecessor(EntityOrderInfo entityOrderInfo, Node_ node) {
        return entityOrderInfo.predecessor(node, listVariableDescriptor, indexVariableSupply, inverseVariableSupply);
    }

    private boolean isMiddleNodeBetween(EntityOrderInfo entityOrderInfo, Node_ start, Node_ middle, Node_ end) {
        return entityOrderInfo.between(start, middle, end, indexVariableSupply, inverseVariableSupply);
    }
}
