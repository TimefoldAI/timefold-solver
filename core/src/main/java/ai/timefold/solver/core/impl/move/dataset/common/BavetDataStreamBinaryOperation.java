package ai.timefold.solver.core.impl.move.dataset.common;

import ai.timefold.solver.core.impl.bavet.common.BavetStreamBinaryOperation;
import ai.timefold.solver.core.impl.move.dataset.AbstractDataStream;

public sealed interface BavetDataStreamBinaryOperation<Solution_>
        extends BavetStreamBinaryOperation<AbstractDataStream<Solution_>> permits BavetIfExistsDataStream {

}
