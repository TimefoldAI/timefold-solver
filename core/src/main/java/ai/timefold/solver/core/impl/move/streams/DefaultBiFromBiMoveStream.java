package ai.timefold.solver.core.impl.move.streams;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.streams.dataset.BiDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveConstructor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiFromBiMoveStream<Solution_, A, B> implements BiMoveStream<Solution_, A, B> {

    private final BiDataset<Solution_, A, B> dataset;

    public DefaultBiFromBiMoveStream(BiDataset<Solution_, A, B> dataset) {
        this.dataset = Objects.requireNonNull(dataset);
    }

    @Override
    public MoveProducer<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return new FromBiUniMoveProducer<>(dataset, Objects.requireNonNull(moveConstructor));
    }

}
