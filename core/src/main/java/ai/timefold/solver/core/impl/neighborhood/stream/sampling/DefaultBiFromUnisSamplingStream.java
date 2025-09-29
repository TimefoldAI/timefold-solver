package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.BiSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.move.FromUniBiMoveStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiFromUnisSamplingStream<Solution_, A, B> implements BiSamplingStream<Solution_, A, B> {

    private final UniDataset<Solution_, A> leftDataset;
    private final UniDataset<Solution_, B> rightDataset;
    private final BiPredicate<A, B> filter;

    public DefaultBiFromUnisSamplingStream(UniDataset<Solution_, A> leftDataset, UniDataset<Solution_, B> rightDataset,
            BiPredicate<A, B> filter) {
        this.leftDataset = Objects.requireNonNull(leftDataset);
        this.rightDataset = Objects.requireNonNull(rightDataset);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public MoveStream<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return new FromUniBiMoveStream<>(leftDataset, rightDataset, filter, Objects.requireNonNull(moveConstructor));
    }

}
