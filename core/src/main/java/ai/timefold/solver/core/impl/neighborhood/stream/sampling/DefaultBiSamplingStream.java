package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.BiSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.move.BiMoveStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniRightDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiSamplingStream<Solution_, A, B> implements BiSamplingStream<Solution_, A, B> {

    private final UniLeftDataset<Solution_, A> leftDataset;
    private final UniRightDataset<Solution_, A, B> rightDataset;

    public DefaultBiSamplingStream(UniLeftDataset<Solution_, A> leftDataset, UniRightDataset<Solution_, A, B> rightDataset) {
        this.leftDataset = Objects.requireNonNull(leftDataset);
        this.rightDataset = Objects.requireNonNull(rightDataset);
    }

    @Override
    public MoveStream<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return new BiMoveStream<>(leftDataset, rightDataset, Objects.requireNonNull(moveConstructor));
    }

}
