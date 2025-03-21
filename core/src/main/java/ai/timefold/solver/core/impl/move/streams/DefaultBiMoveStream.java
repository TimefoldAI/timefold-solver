package ai.timefold.solver.core.impl.move.streams;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveConstructor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiMoveStream<Solution_, A, B> implements BiMoveStream<Solution_, A, B> {

    private final InnerUniMoveStream<Solution_, A> leftMoveStream;
    private final AbstractDataset<Solution_, UniTuple<B>> rightDataset;
    private final BiPredicate<A, B> filter;

    public DefaultBiMoveStream(InnerUniMoveStream<Solution_, A> leftMoveStream,
            AbstractDataset<Solution_, UniTuple<B>> rightDataset,
            BiPredicate<A, B> filter) {
        this.leftMoveStream = Objects.requireNonNull(leftMoveStream);
        this.rightDataset = Objects.requireNonNull(rightDataset);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public MoveProducer<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return new BiMoveProducer<>(leftMoveStream.getDataset(), rightDataset, filter, Objects.requireNonNull(moveConstructor));
    }

    @Override
    public MoveStreamFactory<Solution_> getMoveStreamFactory() {
        return leftMoveStream.getMoveStreamFactory();
    }

}
