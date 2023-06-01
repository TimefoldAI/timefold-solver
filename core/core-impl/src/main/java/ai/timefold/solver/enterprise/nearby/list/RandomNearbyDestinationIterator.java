package ai.timefold.solver.enterprise.nearby.list;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.SelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementRef;
import ai.timefold.solver.enterprise.nearby.common.NearbyDistanceMatrix;
import ai.timefold.solver.enterprise.nearby.common.NearbyRandom;

final class RandomNearbyDestinationIterator extends SelectionIterator<ElementRef> {

    private final NearbyDistanceMatrix<Object, Object> nearbyDistanceMatrix;
    private final NearbyRandom nearbyRandom;
    private final Random workingRandom;
    private final Iterator<?> replayingOriginValueIterator;
    private final Function<Iterator<?>, Object> originFunction;
    private final Function<Object, ElementRef> elementRefFunction;
    private final int nearbySize;

    public RandomNearbyDestinationIterator(NearbyDistanceMatrix<Object, Object> nearbyDistanceMatrix,
            NearbyRandom nearbyRandom, Random workingRandom, Iterator<Object> replayingOriginValueIterator,
            Function<Object, ElementRef> elementRefFunction, long childSize) {
        this(nearbyDistanceMatrix, nearbyRandom, workingRandom, replayingOriginValueIterator, Iterator::next,
                elementRefFunction, childSize);
    }

    public RandomNearbyDestinationIterator(NearbyDistanceMatrix<Object, Object> nearbyDistanceMatrix,
            NearbyRandom nearbyRandom, Random workingRandom, Iterator<?> replayingOriginValueIterator,
            Function<Iterator<?>, Object> originFunction, Function<Object, ElementRef> elementRefFunction,
            long childSize) {
        this.nearbyDistanceMatrix = nearbyDistanceMatrix;
        this.nearbyRandom = nearbyRandom;
        this.workingRandom = workingRandom;
        this.replayingOriginValueIterator = replayingOriginValueIterator;
        this.originFunction = originFunction;
        this.elementRefFunction = elementRefFunction;
        if (childSize > Integer.MAX_VALUE) {
            throw new IllegalStateException("The destinationSelector (" + this
                    + ") has a destinationSize (" + childSize
                    + ") which is higher than Integer.MAX_VALUE.");
        }
        nearbySize = (int) childSize;
    }

    @Override
    public boolean hasNext() {
        return replayingOriginValueIterator.hasNext() && nearbySize > 0;
    }

    @Override
    public ElementRef next() {
        /*
         * The origin iterator is guaranteed to be a replaying iterator.
         * Therefore next() will point to whatever that the related recording iterator was pointing to at the time
         * when its next() was called.
         * As a result, origin here will be constant unless next() on the original recording iterator is called
         * first.
         */
        Object origin = originFunction.apply(replayingOriginValueIterator);
        int nearbyIndex = nearbyRandom.nextInt(workingRandom, nearbySize);
        Object next = nearbyDistanceMatrix.getDestination(origin, nearbyIndex);
        return elementRefFunction.apply(next);
    }

}
