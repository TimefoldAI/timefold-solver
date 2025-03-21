package ai.timefold.solver.core.impl.move.streams;

import java.util.Set;

import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerMoveProducer<Solution_> extends MoveProducer<Solution_> {

    void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> activeDataStreamSet);

}
