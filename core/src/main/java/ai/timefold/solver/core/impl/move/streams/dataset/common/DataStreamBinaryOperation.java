package ai.timefold.solver.core.impl.move.streams.dataset.common;

import ai.timefold.solver.core.impl.bavet.common.BavetStreamBinaryOperation;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface DataStreamBinaryOperation<Solution_>
        extends BavetStreamBinaryOperation<AbstractDataStream<Solution_>>
        permits IfExistsDataStream, JoinDataStream {

}
