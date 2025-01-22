package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.bridge.BavetForeBridgeUniConstraintStream;

public interface BavetStreamBinaryOperation<Solution_> {
    /**
     * @return An instance of {@link BavetForeBridgeUniConstraintStream}.
     */
    BavetAbstractConstraintStream<Solution_> getLeftParent();

    /**
     * @return An instance of {@link BavetForeBridgeUniConstraintStream}.
     */
    BavetAbstractConstraintStream<Solution_> getRightParent();

}
