package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

/**
 * {@link DynamicPropagationQueue} requires the items it carries to extend this class,
 * in order to be able to store metadata on them.
 * This metadata is necessary for efficient operation of the queue.
 */
abstract class AbstractPropagationMetadataCarrier<Tuple_ extends AbstractTuple> {

    public int positionInDirtyList = -1;

    public abstract Tuple_ getTuple();

    public abstract TupleState getState();

    public abstract void setState(TupleState state);

}
