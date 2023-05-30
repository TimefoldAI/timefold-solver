package ai.timefold.solver.enterprise.nearby.value;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicReplayingValueSelector;
import ai.timefold.solver.enterprise.nearby.common.AbstractNearbyDistanceMatrixDemand;
import ai.timefold.solver.enterprise.nearby.common.NearbyDistanceMatrix;
import ai.timefold.solver.enterprise.nearby.common.NearbyRandom;

/**
 * Demands a distance matrix where both the origins and nearby destinations are planning values.
 *
 * @param <Solution_>
 * @param <Origin_> planning values
 * @param <Destination_> planning values
 */
final class ListValueNearbyDistanceMatrixDemand<Solution_, Origin_, Destination_>
        extends
        AbstractNearbyDistanceMatrixDemand<Origin_, Destination_, EntityIndependentValueSelector<Solution_>, MimicReplayingValueSelector<Solution_>> {

    private final ToIntFunction<Origin_> destinationSizeFunction;

    public ListValueNearbyDistanceMatrixDemand(
            NearbyDistanceMeter<Origin_, Destination_> meter,
            NearbyRandom random,
            EntityIndependentValueSelector<Solution_> childValueSelector,
            MimicReplayingValueSelector<Solution_> replayingOriginValueSelector,
            ToIntFunction<Origin_> destinationSizeFunction) {
        super(meter, random, childValueSelector, replayingOriginValueSelector);
        this.destinationSizeFunction = destinationSizeFunction;
    }

    @Override
    protected NearbyDistanceMatrix<Origin_, Destination_> supplyNearbyDistanceMatrix() {
        final long childSize = childSelector.getSize();
        if (childSize > Integer.MAX_VALUE) {
            throw new IllegalStateException("The childSize (" + childSize + ") is higher than Integer.MAX_VALUE.");
        }

        long originSize = replayingSelector.getSize();
        if (originSize > Integer.MAX_VALUE) {
            throw new IllegalStateException("The originValueSelector (" + replayingSelector
                    + ") has a valueSize (" + originSize
                    + ") which is higher than Integer.MAX_VALUE.");
        }
        // Destinations: values extracted from a value selector.
        // List variables use entity independent value selectors, so we can pass null to get and ending iterator.
        Function<Origin_, Iterator<Destination_>> destinationIteratorProvider =
                origin -> (Iterator<Destination_>) childSelector.endingIterator(null);
        NearbyDistanceMatrix<Origin_, Destination_> nearbyDistanceMatrix =
                new NearbyDistanceMatrix<>(meter, (int) originSize, destinationIteratorProvider, destinationSizeFunction);
        // Origins: values extracted from a value selector.
        // Replaying selector's ending iterator uses the recording selector's ending iterator. So, again, null is OK here.
        replayingSelector.endingIterator(null)
                .forEachRemaining(origin -> nearbyDistanceMatrix.addAllDestinations((Origin_) origin));
        return nearbyDistanceMatrix;
    }

}
