package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.util.Pair;

import org.apache.commons.math3.util.CombinatoricsUtils;

final class KOptUtils {

    private KOptUtils() {
    }

    /**
     * Calculate the disjoint k-cycles for {@link KOptDescriptor#removedEdgeIndexToTourOrder()}. <br />
     * <br />
     * Any permutation can be expressed as combination of k-cycles. A k-cycle is a sequence of
     * unique elements (p_1, p_2, ..., p_k) where
     * <ul>
     * <li>p_1 maps to p_2 in the permutation</li>
     * <li>p_2 maps to p_3 in the permutation</li>
     * <li>p_(k-1) maps to p_k in the permutation</li>
     * <li>p_k maps to p_1 in the permutation</li>
     * <li>In general: p_i maps to p_(i+1) in the permutation</li>
     * </ul>
     * For instance, the permutation
     * <ul>
     * <li>1 -> 2</li>
     * <li>2 -> 3</li>
     * <li>3 -> 1</li>
     * <li>4 -> 5</li>
     * <li>5 -> 4</li>
     * </ul>
     * can be expressed as `(1, 2, 3)(4, 5)`.
     *
     * @return The {@link KOptCycle} corresponding to the permutation described by
     *         {@link KOptDescriptor#removedEdgeIndexToTourOrder()}.
     * @param kOptDescriptor The descriptor to calculate cycles for
     */
    static KOptCycle getCyclesForPermutation(KOptDescriptor<?> kOptDescriptor) {
        var cycleCount = 0;
        var removedEdgeIndexToTourOrder = kOptDescriptor.removedEdgeIndexToTourOrder();
        var addedEdgeToOtherEndpoint = kOptDescriptor.addedEdgeToOtherEndpoint();
        var inverseRemovedEdgeIndexToTourOrder = kOptDescriptor.inverseRemovedEdgeIndexToTourOrder();

        var indexToCycle = new int[removedEdgeIndexToTourOrder.length];
        var remaining = new BitSet(removedEdgeIndexToTourOrder.length);
        remaining.set(1, removedEdgeIndexToTourOrder.length, true);

        while (!remaining.isEmpty()) {
            var currentEndpoint = remaining.nextSetBit(0);
            while (remaining.get(currentEndpoint)) {
                indexToCycle[currentEndpoint] = cycleCount;
                remaining.clear(currentEndpoint);

                // Go to the endpoint connected to this one by an added edge
                var currentEndpointTourIndex = removedEdgeIndexToTourOrder[currentEndpoint];
                var nextEndpointTourIndex = addedEdgeToOtherEndpoint[currentEndpointTourIndex];
                currentEndpoint = inverseRemovedEdgeIndexToTourOrder[nextEndpointTourIndex];

                indexToCycle[currentEndpoint] = cycleCount;
                remaining.clear(currentEndpoint);

                // Go to the endpoint after the added edge
                currentEndpoint = currentEndpoint ^ 1;
            }
            cycleCount++;
        }
        return new KOptCycle(cycleCount, indexToCycle);
    }

    static <Node_> List<Pair<Node_, Node_>> getAddedEdgeList(KOptDescriptor<Node_> kOptDescriptor) {
        var k = kOptDescriptor.k();
        List<Pair<Node_, Node_>> out = new ArrayList<>(2 * k);
        var currentEndpoint = 1;

        var removedEdges = kOptDescriptor.removedEdges();
        var addedEdgeToOtherEndpoint = kOptDescriptor.addedEdgeToOtherEndpoint();
        var removedEdgeIndexToTourOrder = kOptDescriptor.removedEdgeIndexToTourOrder();
        var inverseRemovedEdgeIndexToTourOrder = kOptDescriptor.inverseRemovedEdgeIndexToTourOrder();

        // This loop iterates through the new tour created
        while (currentEndpoint != 2 * k + 1) {
            out.add(new Pair<>(removedEdges[currentEndpoint], removedEdges[addedEdgeToOtherEndpoint[currentEndpoint]]));
            var tourIndex = removedEdgeIndexToTourOrder[currentEndpoint];
            var nextEndpointTourIndex = addedEdgeToOtherEndpoint[tourIndex];
            currentEndpoint = inverseRemovedEdgeIndexToTourOrder[nextEndpointTourIndex] ^ 1;
        }
        return out;
    }

    static <Node_> List<Pair<Node_, Node_>> getRemovedEdgeList(KOptDescriptor<Node_> kOptDescriptor) {
        var k = kOptDescriptor.k();
        var removedEdges = kOptDescriptor.removedEdges();
        List<Pair<Node_, Node_>> out = new ArrayList<>(2 * k);
        for (var i = 1; i <= k; i++) {
            out.add(new Pair<>(removedEdges[2 * i - 1], removedEdges[2 * i]));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static <Node_> Function<Node_, Node_> getMultiEntitySuccessorFunction(Node_[] pickedValues,
            ListVariableStateSupply<?> listVariableStateSupply) {
        var entityOrderInfo = EntityOrderInfo.of(pickedValues, listVariableStateSupply);
        return node -> entityOrderInfo.successor(node, listVariableStateSupply);
    }

    public static <Node_> TriPredicate<Node_, Node_, Node_> getBetweenPredicate(IndexVariableSupply indexVariableSupply) {
        return (start, middle, end) -> {
            int startIndex = indexVariableSupply.getIndex(start);
            int middleIndex = indexVariableSupply.getIndex(middle);
            int endIndex = indexVariableSupply.getIndex(end);

            if (startIndex <= endIndex) {
                // test middleIndex in [startIndex, endIndex]
                return startIndex <= middleIndex && middleIndex <= endIndex;
            } else {
                // test middleIndex in [0, endIndex] or middleIndex in [startIndex, listSize)
                return middleIndex >= startIndex || middleIndex <= endIndex;
            }
        };
    }

    public static <Node_> TriPredicate<Node_, Node_, Node_> getMultiEntityBetweenPredicate(Node_[] pickedValues,
            ListVariableStateSupply<?> listVariableStateSupply) {
        var entityOrderInfo = EntityOrderInfo.of(pickedValues, listVariableStateSupply);
        return (start, middle, end) -> entityOrderInfo.between(start, middle, end, listVariableStateSupply);
    }

    public static void flipSubarray(int[] array, int fromIndexInclusive, int toIndexExclusive) {
        if (fromIndexInclusive < toIndexExclusive) {
            final var halfwayPoint = (toIndexExclusive - fromIndexInclusive) >> 1;
            for (var i = 0; i < halfwayPoint; i++) {
                var saved = array[fromIndexInclusive + i];
                array[fromIndexInclusive + i] = array[toIndexExclusive - i - 1];
                array[toIndexExclusive - i - 1] = saved;
            }
        } else {
            var firstHalfSize = array.length - fromIndexInclusive;
            var secondHalfSize = toIndexExclusive;

            // Reverse the combined list firstHalfReversedPath + secondHalfReversedPath
            // For instance, (1, 2, 3)(4, 5, 6, 7, 8, 9) becomes
            // (9, 8, 7)(6, 5, 4, 3, 2, 1)
            var totalLength = firstHalfSize + secondHalfSize;

            // Used to rotate the list to put the first element back in its original position
            for (var i = 0; (i < totalLength >> 1); i++) {
                int firstHalfIndex;
                int secondHalfIndex;

                if (i < firstHalfSize) {
                    if (i < secondHalfSize) {
                        firstHalfIndex = fromIndexInclusive + i;
                        secondHalfIndex = secondHalfSize - i - 1;
                    } else {
                        firstHalfIndex = fromIndexInclusive + i;
                        secondHalfIndex = array.length - (i - secondHalfSize) - 1;
                    }
                } else {
                    firstHalfIndex = i - firstHalfSize;
                    secondHalfIndex = secondHalfSize - i - 1;
                }

                var saved = array[firstHalfIndex];
                array[firstHalfIndex] = array[secondHalfIndex];
                array[secondHalfIndex] = saved;
            }
        }
    }

    /**
     * Returns the number of unique ways a K-Opt can add K edges without reinserting a removed edge.
     *
     * @param k How many edges were removed/will be added
     * @return the number of unique ways a K-Opt can add K edges without reinserting a removed edge.
     */
    public static long getPureKOptMoveTypes(int k) {
        // This calculates the item at index k for the sequence https://oeis.org/A061714
        long totalTypes = 0;
        for (var i = 1; i < k; i++) {
            for (var j = 0; j <= i; j++) {
                var sign = ((k + j - 1) % 2 == 0) ? 1 : -1;
                totalTypes += sign * CombinatoricsUtils.binomialCoefficient(i, j) * CombinatoricsUtils.factorial(j) * (1L << j);
            }
        }
        return totalTypes;
    }
}
