package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.BiSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.move.FromBiUniMoveStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.BiDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiFromBiSamplingStream<Solution_, A, B> implements BiSamplingStream<Solution_, A, B> {

    private final BiDataset<Solution_, A, B> dataset;

    public DefaultBiFromBiSamplingStream(BiDataset<Solution_, A, B> dataset) {
        this.dataset = Objects.requireNonNull(dataset);
    }

    @Override
    public MoveStream<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return new FromBiUniMoveStream<>(dataset, Objects.requireNonNull(moveConstructor));
    }

}
