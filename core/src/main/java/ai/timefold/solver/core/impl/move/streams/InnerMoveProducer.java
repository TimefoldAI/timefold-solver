package ai.timefold.solver.core.impl.move.streams;

import java.util.Set;

import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerMoveProducer<Solution_> extends MoveProducer<Solution_> {

    @Override
    MoveIterable<Solution_> getMoveIterable(MoveStreamSession<Solution_> moveStreamSession);

    void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> activeDataStreamSet);

}
