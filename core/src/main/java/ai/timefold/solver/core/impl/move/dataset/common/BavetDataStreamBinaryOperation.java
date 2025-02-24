package ai.timefold.solver.core.impl.move.dataset.common;

import ai.timefold.solver.core.impl.move.dataset.AbstractDataStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetStreamBinaryOperation;

public sealed interface BavetDataStreamBinaryOperation<Solution_>
        extends BavetStreamBinaryOperation<AbstractDataStream<Solution_>>
        permits BavetIfExistsDataStream {

}
