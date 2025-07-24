package ai.timefold.solver.core.impl.move.streams.dataset.common;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;

import org.jspecify.annotations.NullMarked;

@NullMarked
public non-sealed interface JoinDataStream<Solution_>
        extends DataStreamBinaryOperation<Solution_>, TupleSource {

}
