package ai.timefold.solver.core.impl.move.streams.dataset.common;

import ai.timefold.solver.core.impl.bavet.common.BavetStreamBinaryOperation;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataStream;

public sealed interface BavetDataStreamBinaryOperation<Solution_>
        extends BavetStreamBinaryOperation<AbstractDataStream<Solution_>>
        permits BavetIfExistsDataStream {

}
