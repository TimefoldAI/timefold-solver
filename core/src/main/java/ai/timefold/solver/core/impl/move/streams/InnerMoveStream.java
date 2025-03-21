package ai.timefold.solver.core.impl.move.streams;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerMoveStream<Solution_, Out_ extends AbstractTuple> extends MoveStream<Solution_> {

    AbstractDataset<Solution_, Out_> getDataset();

}
