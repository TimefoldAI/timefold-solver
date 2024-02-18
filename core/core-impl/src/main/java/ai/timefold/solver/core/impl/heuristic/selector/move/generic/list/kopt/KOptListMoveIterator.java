package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;

final class KOptListMoveIterator<Solution_, Node_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final Random workingRandom;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final EntityIndependentValueSelector<Node_> originSelector;
    private final EntityIndependentValueSelector<Node_> valueSelector;
    private final int minK;
    private final int[] pickedKDistribution;
    private final int pickedKDistributionSum;
    private final int maxCyclesPatchedInInfeasibleMove;

    public KOptListMoveIterator(Random workingRandom, ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableStateSupply<Solution_> listVariableStateSupply, EntityIndependentValueSelector<Node_> originSelector,
            EntityIndependentValueSelector<Node_> valueSelector, int minK, int maxK, int[] pickedKDistribution) {
        this.workingRandom = workingRandom;
        this.listVariableDescriptor = listVariableDescriptor;
        this.listVariableStateSupply = listVariableStateSupply;
        this.originSelector = originSelector;
        this.valueSelector = valueSelector;
        this.minK = minK;
        this.pickedKDistribution = pickedKDistribution;
        var tmpPickedKDistributionSum = 0;
        for (var relativeDistributionAmount : pickedKDistribution) {
            tmpPickedKDistributionSum += relativeDistributionAmount;
        }
        this.pickedKDistributionSum = tmpPickedKDistributionSum;
        this.maxCyclesPatchedInInfeasibleMove = maxK;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        var locationInDistribution = workingRandom.nextInt(pickedKDistributionSum);
        var indexInDistribution = 0;
        while (locationInDistribution >= pickedKDistribution[indexInDistribution]) {
            locationInDistribution -= pickedKDistribution[indexInDistribution];
            indexInDistribution++;
        }
        var k = minK + indexInDistribution;
        if (k == 2) {
            return pickTwoOptMove();
        }
        var descriptor = pickKOptMove(k);
        if (descriptor == null) {
            // Was unable to find a K-Opt move
            return NoChangeMove.getInstance();
        }
        return descriptor.getKOptListMove(listVariableStateSupply);
    }

    private Move<Solution_> pickTwoOptMove() {
        @SuppressWarnings("unchecked")
        var originIterator = (Iterator<Node_>) originSelector.iterator();
        if (!originIterator.hasNext()) {
            return NoChangeMove.getInstance();
        }
        @SuppressWarnings("unchecked")
        var valueIterator = (Iterator<Node_>) valueSelector.iterator();
        if (!valueIterator.hasNext()) {
            return NoChangeMove.getInstance();
        }

        Object firstValue = originIterator.next();
        Object secondValue = valueIterator.next();

        var firstElementLocation = (LocationInList) listVariableStateSupply.getLocationInList(firstValue);
        var secondElementLocation = (LocationInList) listVariableStateSupply.getLocationInList(secondValue);
        return new TwoOptListMove<>(listVariableDescriptor, firstElementLocation.entity(), secondElementLocation.entity(),
                firstElementLocation.index(), secondElementLocation.index());
    }

    @SuppressWarnings("unchecked")
    private Iterator<Node_> getValuesOnSelectedEntitiesIterator(Node_[] pickedValues) {
        var entityOrderInfo = EntityOrderInfo.of(pickedValues, listVariableStateSupply);
        return (Iterator<Node_>) workingRandom.ints(0, entityOrderInfo.entities().length)
                .mapToObj(index -> {
                    var entity = entityOrderInfo.entities()[index];
                    return listVariableDescriptor.getRandomUnpinnedElement(entity, workingRandom);
                })
                .iterator();
    }

    @SuppressWarnings("unchecked")
    private KOptDescriptor<Node_> pickKOptMove(int k) {
        // The code in the paper used 1-index arrays
        var pickedValues = (Node_[]) new Object[2 * k + 1];
        var originIterator = (Iterator<Node_>) originSelector.iterator();

        pickedValues[1] = originIterator.next();
        if (pickedValues[1] == null) {
            return null;
        }
        var remainingAttempts = 20;
        while (remainingAttempts > 0
                && listVariableDescriptor
                        .getUnpinnedSubListSize(listVariableStateSupply.getInverseSingleton(pickedValues[1])) < 2) {
            do {
                if (!originIterator.hasNext()) {
                    return null;
                }
                pickedValues[1] = originIterator.next();
                remainingAttempts--;
            } while ((pickedValues[1] == null));
        }

        if (remainingAttempts == 0) {
            // could not find a value in a list with more than 1 element
            return null;
        }

        var entityOrderInfo = EntityOrderInfo.of(pickedValues, listVariableStateSupply);
        pickedValues[2] = workingRandom.nextBoolean() ? getNodeSuccessor(entityOrderInfo, pickedValues[1])
                : getNodePredecessor(entityOrderInfo, pickedValues[1]);

        if (isNodeEndpointOfList(pickedValues[1]) || isNodeEndpointOfList(pickedValues[2])) {
            return pickKOptMoveRec(getValuesOnSelectedEntitiesIterator(pickedValues), entityOrderInfo, pickedValues, 2, k,
                    false);
        } else {
            return pickKOptMoveRec((Iterator<Node_>) valueSelector.iterator(), entityOrderInfo, pickedValues, 2, k, true);
        }
    }

    private KOptDescriptor<Node_> pickKOptMoveRec(Iterator<Node_> valueIterator,
            EntityOrderInfo entityOrderInfo,
            Node_[] pickedValues,
            int pickedSoFar,
            int k,
            boolean canSelectNewEntities) {
        var previousRemovedEdgeEndpoint = pickedValues[2 * pickedSoFar - 2];
        Node_ nextRemovedEdgePoint, nextRemovedEdgeOppositePoint;

        var remainingAttempts = (k - pickedSoFar + 3) * 2;
        while (remainingAttempts > 0) {
            nextRemovedEdgePoint = valueIterator.next();
            var newEntityOrderInfo =
                    entityOrderInfo.withNewNode(nextRemovedEdgePoint, listVariableStateSupply);
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
                        entityOrderInfo.withNewNode(nextRemovedEdgePoint, listVariableStateSupply);
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
                var descriptor = pickKOptMoveRec(valueIterator, newEntityOrderInfo, pickedValues,
                        pickedSoFar + 1, k, canSelectNewEntities);
                if (descriptor != null && descriptor.isFeasible(minK, maxCyclesPatchedInInfeasibleMove)) {
                    return descriptor;
                }
            } else {
                var descriptor = new KOptDescriptor<Node_>(pickedValues,
                        KOptUtils.getMultiEntitySuccessorFunction(pickedValues,
                                listVariableStateSupply),
                        KOptUtils.getMultiEntityBetweenPredicate(pickedValues,
                                listVariableStateSupply));
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
        var removedEdgeIndexToTourOrder = descriptor.removedEdgeIndexToTourOrder();
        var valueIterator = getValuesOnSelectedEntitiesIterator(oldRemovedEdges);
        var cycleInfo = KOptUtils.getCyclesForPermutation(descriptor);
        var cycleCount = cycleInfo.cycleCount();
        var cycle = cycleInfo.indexToCycleIdentifier();

        // If cycleCount != 1,
        // we are changing an infeasible k-opt move that results in cycleCount cycles
        // into a (k+cycleCount) move.
        // If the k+cycleCount > maxK, we should ignore generating the move
        // Note: maxCyclesPatchedInInfeasibleMove = maxK
        if (cycleCount == 1 || k + cycleCount > maxCyclesPatchedInInfeasibleMove) {
            return descriptor;
        }
        var currentCycle =
                getShortestCycleIdentifier(entityOrderInfo, oldRemovedEdges, cycle, removedEdgeIndexToTourOrder, cycleCount, k);

        for (var i = 0; i < k; i++) {
            if (cycle[removedEdgeIndexToTourOrder[2 * i]] == currentCycle) {
                var sStart = oldRemovedEdges[removedEdgeIndexToTourOrder[2 * i]];
                var sStop = oldRemovedEdges[removedEdgeIndexToTourOrder[2 * i + 1]];
                var attemptRemaining = k;
                for (s1 = sStart; s1 != sStop; s1 = s2) {
                    attemptRemaining--;
                    if (attemptRemaining == 0) {
                        break;
                    }
                    var removedEdges = Arrays.copyOf(oldRemovedEdges, oldRemovedEdges.length + 2);

                    removedEdges[2 * k + 1] = s1;
                    s2 = getNodeSuccessor(entityOrderInfo, s1);
                    removedEdges[2 * k + 2] = s2;
                    var addedEdgeToOtherEndpoint =
                            Arrays.copyOf(KOptDescriptor.computeInEdgesForSequentialMove(oldRemovedEdges),
                                    removedEdges.length + 2 + (2 * cycleCount));
                    for (var newEdge = removedEdges.length; newEdge < addedEdgeToOtherEndpoint.length - 2; newEdge++) {
                        addedEdgeToOtherEndpoint[newEdge] = newEdge + 2;
                    }
                    addedEdgeToOtherEndpoint[addedEdgeToOtherEndpoint.length - 1] = addedEdgeToOtherEndpoint.length - 3;
                    addedEdgeToOtherEndpoint[addedEdgeToOtherEndpoint.length - 2] = addedEdgeToOtherEndpoint.length - 4;
                    var newMove = patchCyclesRec(valueIterator, descriptor, entityOrderInfo, removedEdges,
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
        var cycleSaved = new Integer[1 + 2 * k];
        var removedEdges = Arrays.copyOf(oldRemovedEdges, oldRemovedEdges.length + 2);

        s1 = removedEdges[2 * k + 1];
        s2 = removedEdges[i = 2 * (k + patchedCycleCount) - 2];
        addedEdgeToOtherEndpoint[addedEdgeToOtherEndpoint[i] = i + 1] = i;

        for (i = 1; i <= 2 * k; i++) {
            cycleSaved[i] = cycle[i];
        }
        s3 = valueIterator.next();
        var remainingAttempts = cycleCount * 2;
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
            var recursiveCall =
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
                    KOptUtils.getMultiEntitySuccessorFunction(removedEdges, listVariableStateSupply),
                    KOptUtils.getMultiEntityBetweenPredicate(removedEdges, listVariableStateSupply));
        }
        return originalMove;
    }

    int findCycleIdentifierForNode(EntityOrderInfo entityOrderInfo, Node_ value, Node_[] pickedValues, int[] permutation,
            int[] indexToCycle) {
        for (var i = 1; i < pickedValues.length; i++) {
            if (isMiddleNodeBetween(entityOrderInfo, pickedValues[permutation[i - 1]], value, pickedValues[permutation[i]])) {
                return indexToCycle[permutation[i]];
            }
        }
        throw new IllegalStateException("Cannot find cycle the " + value + " belongs to");
    }

    int getShortestCycleIdentifier(EntityOrderInfo entityOrderInfo, Object[] removeEdgeEndpoints, int[] endpointIndexToCycle,
            int[] removeEdgeEndpointIndexToTourOrder, int cycleCount, int k) {
        int i;
        var minCycleIdentifier = 0;
        var minSize = Integer.MAX_VALUE;
        var size = new int[cycleCount + 1];

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
        var startElementLocation = (LocationInList) listVariableStateSupply.getLocationInList(from);
        var endElementLocation = (LocationInList) listVariableStateSupply.getLocationInList(to);
        int startEntityIndex = entityToEntityIndex.get(startElementLocation.entity());
        int endEntityIndex = entityToEntityIndex.get(endElementLocation.entity());
        var offsets = entityOrderInfo.offsets();
        var startIndex = offsets[startEntityIndex] + startElementLocation.index();
        var endIndex = offsets[endEntityIndex] + endElementLocation.index();

        if (startIndex <= endIndex) {
            return endIndex - startIndex;
        } else {
            var entities = entityOrderInfo.entities();
            var totalRouteSize =
                    offsets[offsets.length - 1] + listVariableDescriptor.getListSize(entities[entities.length - 1]);
            return totalRouteSize - startIndex + endIndex;
        }
    }

    private boolean isEdgeAlreadyAdded(Object[] pickedValues, Object ta, Object tb, int k) {
        var i = 2 * k;
        while ((i -= 2) > 0) {
            if ((ta == pickedValues[i] && tb == pickedValues[i + 1]) ||
                    (ta == pickedValues[i + 1] && tb == pickedValues[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isEdgeAlreadyDeleted(Object[] pickedValues, Object ta, Object tb, int k) {
        var i = 2 * k + 2;
        while ((i -= 2) > 0) {
            if ((ta == pickedValues[i - 1] && tb == pickedValues[i]) ||
                    (ta == pickedValues[i] && tb == pickedValues[i - 1])) {
                return true;
            }
        }
        return false;
    }

    private boolean isNodeEndpointOfList(Object node) {
        var elementLocation = (LocationInList) listVariableStateSupply.getLocationInList(node);
        var index = elementLocation.index();
        var firstUnpinnedIndex = listVariableDescriptor.getFirstUnpinnedIndex(elementLocation.entity());
        if (index == firstUnpinnedIndex) {
            return true;
        }
        var size = listVariableDescriptor.getListSize(elementLocation.entity());
        return index == size - 1;
    }

    private Node_ getNodeSuccessor(EntityOrderInfo entityOrderInfo, Node_ node) {
        return entityOrderInfo.successor(node, listVariableStateSupply);
    }

    private Node_ getNodePredecessor(EntityOrderInfo entityOrderInfo, Node_ node) {
        return entityOrderInfo.predecessor(node, listVariableStateSupply);
    }

    private boolean isMiddleNodeBetween(EntityOrderInfo entityOrderInfo, Node_ start, Node_ middle, Node_ end) {
        return entityOrderInfo.between(start, middle, end, listVariableStateSupply);
    }
}
