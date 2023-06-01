package ai.timefold.solver.enterprise.nearby.value;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.enterprise.nearby.common.AbstractNearbyDistanceMatrixDemand;
import ai.timefold.solver.enterprise.nearby.common.NearbyDistanceMatrix;
import ai.timefold.solver.enterprise.nearby.common.NearbyRandom;

/**
 * Demands a distance matrix where the origins are planning entities and nearby destinations are either entities or values.
 * <p>
 * Calculating {@link NearbyDistanceMatrix} is very expensive,
 * therefore we want to reuse it as much as possible.
 * <p>
 * In cases where the demand represents the same nearby selector (as defined by
 * {@link ValueNearbyDistanceMatrixDemand#equals(Object)})
 * the {@link SupplyManager} ensures that the same supply instance is returned
 * with the pre-computed {@link NearbyDistanceMatrix}.
 *
 * @param <Solution_>
 * @param <Origin_> planning entities
 * @param <Destination_> planning entities XOR planning values
 */
final class ValueNearbyDistanceMatrixDemand<Solution_, Origin_, Destination_>
        extends AbstractNearbyDistanceMatrixDemand<Origin_, Destination_, ValueSelector<Solution_>, EntitySelector<Solution_>> {

    private final ToIntFunction<Origin_> destinationSizeFunction;

    public ValueNearbyDistanceMatrixDemand(NearbyDistanceMeter<Origin_, Destination_> meter, NearbyRandom random,
            ValueSelector<Solution_> childSelector, EntitySelector<Solution_> replayingOriginEntitySelector,
            ToIntFunction<Origin_> destinationSizeFunction) {
        super(meter, random, childSelector, replayingOriginEntitySelector);
        this.destinationSizeFunction = destinationSizeFunction;
    }

    @Override
    protected NearbyDistanceMatrix<Origin_, Destination_> supplyNearbyDistanceMatrix() {
        long originSize = replayingSelector.getSize();
        if (originSize > Integer.MAX_VALUE) {
            throw new IllegalStateException("The originEntitySelector (" + replayingSelector
                    + ") has an entitySize (" + originSize
                    + ") which is higher than Integer.MAX_VALUE.");
        }
        // Destinations: values extracted a value selector.
        Function<Origin_, Iterator<Destination_>> destinationIteratorProvider =
                origin -> (Iterator<Destination_>) childSelector.endingIterator(origin);
        NearbyDistanceMatrix<Origin_, Destination_> nearbyDistanceMatrix =
                new NearbyDistanceMatrix<>(meter, (int) originSize, destinationIteratorProvider, destinationSizeFunction);
        // Origins: entities extracted from an entity selector.
        replayingSelector.endingIterator()
                .forEachRemaining(origin -> nearbyDistanceMatrix.addAllDestinations((Origin_) origin));
        return nearbyDistanceMatrix;
    }

}
