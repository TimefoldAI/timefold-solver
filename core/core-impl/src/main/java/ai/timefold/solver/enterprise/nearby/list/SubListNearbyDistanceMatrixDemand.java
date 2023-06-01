package ai.timefold.solver.enterprise.nearby.list;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementDestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.MimicReplayingSubListSelector;
import ai.timefold.solver.enterprise.nearby.common.AbstractNearbyDistanceMatrixDemand;
import ai.timefold.solver.enterprise.nearby.common.NearbyDistanceMatrix;
import ai.timefold.solver.enterprise.nearby.common.NearbyRandom;

/**
 * Demands a distance matrix where the origins are planning values and nearby destinations are both planning entities and
 * values.
 * <p>
 * Calculating {@link NearbyDistanceMatrix} is very expensive,
 * therefore we want to reuse it as much as possible.
 * <p>
 * In cases where the demand represents the same nearby selector (as defined by
 * {@link SubListNearbyDistanceMatrixDemand#equals(Object)})
 * the {@link SupplyManager} ensures that the same supply instance is returned
 * with the pre-computed {@link NearbyDistanceMatrix}.
 *
 * @param <Solution_>
 * @param <Origin_> planning values
 * @param <Destination_> mix of planning entities and planning values
 */
final class SubListNearbyDistanceMatrixDemand<Solution_, Origin_, Destination_>
        extends
        AbstractNearbyDistanceMatrixDemand<Origin_, Destination_, ElementDestinationSelector<Solution_>, MimicReplayingSubListSelector<Solution_>> {

    private final ToIntFunction<Origin_> destinationSizeFunction;

    public SubListNearbyDistanceMatrixDemand(
            NearbyDistanceMeter<Origin_, Destination_> meter,
            NearbyRandom random,
            ElementDestinationSelector<Solution_> childDestinationSelector,
            MimicReplayingSubListSelector<Solution_> replayingOriginSubListSelector,
            ToIntFunction<Origin_> destinationSizeFunction) {
        super(meter, random, childDestinationSelector, replayingOriginSubListSelector);
        this.destinationSizeFunction = destinationSizeFunction;
    }

    @Override
    protected NearbyDistanceMatrix<Origin_, Destination_> supplyNearbyDistanceMatrix() {
        final long childSize = childSelector.getSize();
        if (childSize > Integer.MAX_VALUE) {
            throw new IllegalStateException("The childSize (" + childSize + ") is higher than Integer.MAX_VALUE.");
        }

        long originSize = replayingSelector.getValueCount();
        if (originSize > Integer.MAX_VALUE) {
            throw new IllegalStateException("The originSubListSelector (" + childSelector
                    + ") has a subListSize (" + originSize
                    + ") which is higher than Integer.MAX_VALUE.");
        }
        // Destinations: mix of planning entities and values extracted from a destination selector.
        // Distance "matrix" elements must be user classes (entities and values) because they are exposed
        // to the user-implemented NearbyDistanceMeter. Therefore, we cannot insert ElementRefs in the matrix.
        // For this reason, destination selector's endingIterator() returns entities and values produced by
        // its child selectors.
        Function<Origin_, Iterator<Destination_>> destinationIteratorProvider =
                origin -> (Iterator<Destination_>) childSelector.endingIterator();
        NearbyDistanceMatrix<Origin_, Destination_> nearbyDistanceMatrix =
                new NearbyDistanceMatrix<>(meter, (int) originSize, destinationIteratorProvider, destinationSizeFunction);
        // Origins: values extracted from a subList selector.
        replayingSelector.endingValueIterator()
                .forEachRemaining(origin -> nearbyDistanceMatrix.addAllDestinations((Origin_) origin));
        return nearbyDistanceMatrix;
    }

}
