package ai.timefold.solver.enterprise.nearby.entity;

import java.util.Iterator;
import java.util.ListIterator;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.MimicReplayingEntitySelector;
import ai.timefold.solver.enterprise.nearby.common.AbstractNearbyDistanceMatrixDemand;
import ai.timefold.solver.enterprise.nearby.common.AbstractNearbySelector;
import ai.timefold.solver.enterprise.nearby.common.NearbyRandom;
import ai.timefold.solver.enterprise.nearby.common.RandomNearbyIterator;

public final class NearEntityNearbyEntitySelector<Solution_>
        extends AbstractNearbySelector<Solution_, EntitySelector<Solution_>, EntitySelector<Solution_>>
        implements EntitySelector<Solution_> {

    // TODO deactivate me when appropriate; consider if this field needs to be included in selector equality
    private final boolean discardNearbyIndexZero = true;

    public NearEntityNearbyEntitySelector(EntitySelector<Solution_> childEntitySelector,
            EntitySelector<Solution_> originEntitySelector, NearbyDistanceMeter<?, ?> nearbyDistanceMeter,
            NearbyRandom nearbyRandom, boolean randomSelection) {
        super(childEntitySelector, originEntitySelector, nearbyDistanceMeter, nearbyRandom, randomSelection);
        if (!childEntitySelector.getEntityDescriptor().getEntityClass().isAssignableFrom(
                originEntitySelector.getEntityDescriptor().getEntityClass())) {
            throw new IllegalArgumentException("The entitySelector (" + this
                    + ") has an entityClass ("
                    + childEntitySelector.getEntityDescriptor().getEntityClass()
                    + ") which is not a superclass of the originEntitySelector's entityClass ("
                    + originEntitySelector.getEntityDescriptor().getEntityClass() + ").");
        }
    }

    @Override
    protected EntitySelector<Solution_> castReplayingSelector(Object uncastReplayingSelector) {
        if (!(uncastReplayingSelector instanceof MimicReplayingEntitySelector)) {
            // In order to select a nearby entity, we must first have something to be near by.
            throw new IllegalStateException("Impossible state: Nearby entity selector (" + this +
                    ") did not receive a replaying entity selector (" + uncastReplayingSelector + ").");
        }
        return (EntitySelector<Solution_>) uncastReplayingSelector;
    }

    @Override
    protected AbstractNearbyDistanceMatrixDemand<?, ?, ?, ?> createDemand() {
        return new EntityNearbyDistanceMatrixDemand<>(
                nearbyDistanceMeter,
                nearbyRandom,
                childSelector,
                replayingSelector,
                origin -> computeDestinationSize(childSelector.getSize()));
    }

    private int computeDestinationSize(long childSize) {
        int destinationSize = (int) childSize;
        if (randomSelection) {
            // Reduce RAM memory usage by reducing destinationSize if nearbyRandom will never select a higher value
            int overallSizeMaximum = nearbyRandom.getOverallSizeMaximum();
            if (discardNearbyIndexZero) {
                if (overallSizeMaximum != Integer.MAX_VALUE) {
                    overallSizeMaximum++;
                }
            }
            if (destinationSize > overallSizeMaximum) {
                destinationSize = overallSizeMaximum;
            }
        }
        return destinationSize;
    }

    @Override
    public EntityDescriptor<Solution_> getEntityDescriptor() {
        return childSelector.getEntityDescriptor();
    }

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public long getSize() {
        return childSelector.getSize() - (discardNearbyIndexZero ? 1 : 0);
    }

    @Override
    public Iterator<Object> iterator() {
        Iterator<Object> replayingOriginEntityIterator = replayingSelector.iterator();
        if (!randomSelection) {
            return new OriginalNearbyEntityIterator(nearbyDistanceMatrix, replayingOriginEntityIterator,
                    childSelector.getSize(), discardNearbyIndexZero);
        } else {
            return new RandomNearbyIterator(nearbyDistanceMatrix, nearbyRandom, workingRandom,
                    replayingOriginEntityIterator, childSelector.getSize(), discardNearbyIndexZero);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        // TODO Not yet implemented
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        // TODO Not yet implemented
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Object> endingIterator() {
        // TODO It should probably use nearby order
        // It must include the origin entity too
        return childSelector.endingIterator();
    }

}
