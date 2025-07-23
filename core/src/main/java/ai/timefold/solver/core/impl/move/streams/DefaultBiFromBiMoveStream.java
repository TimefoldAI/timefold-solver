package ai.timefold.solver.core.impl.move.streams;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveConstructor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiFromBiMoveStream<Solution_, A, B> implements BiMoveStream<Solution_, A, B> {

    private final AbstractDataset<Solution_, BiTuple<A, B>> dataset;

    public DefaultBiFromBiMoveStream(AbstractDataset<Solution_, BiTuple<A, B>> dataset) {
        this.dataset = Objects.requireNonNull(dataset);
    }

    @Override
    public MoveProducer<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return null; // TODO
    }

}
