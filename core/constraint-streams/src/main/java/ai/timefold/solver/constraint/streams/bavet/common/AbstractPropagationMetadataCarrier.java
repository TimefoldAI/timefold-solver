package ai.timefold.solver.constraint.streams.bavet.common;

/**
 * {@link AbstractDynamicPropagationQueue} requires the items it carries to extend this class,
 * in order to be able to store metadata on them.
 * This metadata is necessary for efficient operation of the queue.
 */
sealed abstract class AbstractPropagationMetadataCarrier
        permits AbstractGroup, ExistsCounter {

    public PropagationType propagationType;
    public int positionInDirtyList;

    protected AbstractPropagationMetadataCarrier() {
        clearMetadata();
    }

    public enum PropagationType {

        NONE,
        INSERT,
        UPDATE,
        RETRACT;

    }

    public void clearMetadata() {
        propagationType = PropagationType.NONE;
        positionInDirtyList = -1;
    }

}
