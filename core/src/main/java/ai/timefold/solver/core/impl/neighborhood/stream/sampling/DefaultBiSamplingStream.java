package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.BiSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.move.BiMoveStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.BiEnumeratingJoinerComber;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBiSamplingStream<Solution_, A, B> implements BiSamplingStream<Solution_, A, B> {

    private final UniDataset<Solution_, A> leftDataset;
    private final UniDataset<Solution_, B> rightDataset;
    private final BiEnumeratingJoinerComber<Solution_, A, B> comber;

    public DefaultBiSamplingStream(UniDataset<Solution_, A> leftDataset, UniDataset<Solution_, B> rightDataset,
            BiEnumeratingJoinerComber<Solution_, A, B> comber) {
        this.leftDataset = Objects.requireNonNull(leftDataset);
        this.rightDataset = Objects.requireNonNull(rightDataset);
        this.comber = Objects.requireNonNull(comber);
    }

    @Override
    public MoveStream<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor) {
        return new BiMoveStream<>(leftDataset, rightDataset, comber, Objects.requireNonNull(moveConstructor));
    }

}
