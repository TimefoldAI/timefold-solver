package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.BitSet;

/**
 * {@link AbstractDynamicPropagationQueue} requires the items it carries to extend this class,
 * in order to be able to store metadata on them.
 * This metadata is necessary for efficient operation of the queue.
 */
sealed abstract class AbstractPropagationMetadataCarrier
        permits AbstractGroup, ExistsCounter {

    public BitSet currentQueue = null;
    public int positionInDirtyList = -1;

}
