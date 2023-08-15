package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeUniConstraintStream;

public interface BavetIfExistsConstraintStream<Solution_> {

    BavetAbstractConstraintStream<Solution_> getLeftParent();

    /**
     *
     * @return An instance of {@link BavetForeBridgeUniConstraintStream}.
     */
    BavetAbstractConstraintStream<Solution_> getRightParent();

}
