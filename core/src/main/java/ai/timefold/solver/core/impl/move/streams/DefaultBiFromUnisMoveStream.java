package ai.timefold.solver.core.impl.move.streams;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.move.streams.dataset.UniDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveConstructor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiFromUnisMoveStream<Solution_, A, B> implements BiMoveStream<Solution_, A, B> {

    private final UniDataset<Solution_, A> leftDataset;
    private final UniDataset<Solution_, B> rightDataset;
    private final BiPredicate<A, B> filter;

    public DefaultBiFromUnisMoveStream(UniDataset<Solution_, A> leftDataset, UniDataset<Solution_, B> rightDataset,
            BiPredicate<A, B> filter) {
        this.leftDataset = Objects.requireNonNull(leftDataset);
        this.rightDataset = Objects.requireNonNull(rightDataset);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public MoveProducer<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return new FromUniBiMoveProducer<>(leftDataset, rightDataset, filter, Objects.requireNonNull(moveConstructor));
    }

}
