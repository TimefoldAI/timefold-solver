package ai.timefold.solver.core.impl.move.streams;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveConstructor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiFromUnisMoveStream<Solution_, A, B> implements BiMoveStream<Solution_, A, B> {

    private final AbstractDataset<Solution_, UniTuple<A>> leftDataset;
    private final AbstractDataset<Solution_, UniTuple<B>> rightDataset;
    private final BiPredicate<A, B> filter;

    public DefaultBiFromUnisMoveStream(AbstractDataset<Solution_, UniTuple<A>> leftDataset,
            AbstractDataset<Solution_, UniTuple<B>> rightDataset,
            BiPredicate<A, B> filter) {
        this.leftDataset = Objects.requireNonNull(leftDataset);
        this.rightDataset = Objects.requireNonNull(rightDataset);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public MoveProducer<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return new BiMoveProducer<>(leftDataset, rightDataset, filter, Objects.requireNonNull(moveConstructor));
    }

}
