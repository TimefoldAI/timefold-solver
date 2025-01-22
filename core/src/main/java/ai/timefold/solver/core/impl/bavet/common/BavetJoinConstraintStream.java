package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.bridge.BavetForeBridgeUniConstraintStream;

public interface BavetJoinConstraintStream<Solution_>
        extends BavetStreamBinaryOperation<Solution_>, TupleSource {

    /**
     *
     * @return An instance of {@link BavetForeBridgeUniConstraintStream}.
     */
    BavetAbstractConstraintStream<Solution_> getLeftParent();

    /**
     *
     * @return An instance of {@link BavetForeBridgeUniConstraintStream}.
     */
    BavetAbstractConstraintStream<Solution_> getRightParent();

}
