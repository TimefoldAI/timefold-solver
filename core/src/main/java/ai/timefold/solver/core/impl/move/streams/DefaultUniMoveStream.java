package ai.timefold.solver.core.impl.move.streams;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.move.streams.dataset.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.UniDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultUniMoveStream<Solution_, A> implements InnerUniMoveStream<Solution_, A> {

    private final DefaultMoveStreamFactory<Solution_> moveStreamFactory;
    private final UniDataset<Solution_, A> dataset;

    public DefaultUniMoveStream(DefaultMoveStreamFactory<Solution_> moveStreamFactory, UniDataset<Solution_, A> dataset) {
        this.moveStreamFactory = Objects.requireNonNull(moveStreamFactory);
        this.dataset = Objects.requireNonNull(dataset);
    }

    @Override
    public <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream, BiPredicate<A, B> filter) {
        return new DefaultBiMoveStream<>(this, ((AbstractUniDataStream<Solution_, B>) uniDataStream).createDataset(), filter);
    }

    @Override
    public UniDataset<Solution_, A> getDataset() {
        return dataset;
    }

    @Override
    public MoveStreamFactory<Solution_> getMoveStreamFactory() {
        return moveStreamFactory;
    }

}
