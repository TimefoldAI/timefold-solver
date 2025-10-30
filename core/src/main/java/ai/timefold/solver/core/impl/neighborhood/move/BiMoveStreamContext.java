package ai.timefold.solver.core.impl.neighborhood.move;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingPredicate;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.BiEnumeratingJoinerComber;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.DefaultBiEnumeratingJoiner;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDatasetInstance;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record BiMoveStreamContext<Solution_, A, B>(DefaultNeighborhoodSession<Solution_> neighborhoodSession,
        UniDataset<Solution_, A> leftDataset, UniDataset<Solution_, B> rightDataset,
        BiEnumeratingJoinerComber<Solution_, A, B> joinerComber,
        BiMoveConstructor<Solution_, A, B> moveConstructor) {

    public UniDatasetInstance<Solution_, A> getLeftDatasetInstance() {
        return neighborhoodSession.getDatasetInstance(leftDataset);
    }

    public UniDatasetInstance<Solution_, B> getRightDatasetInstance() {
        return neighborhoodSession.getDatasetInstance(rightDataset);
    }

    public DefaultBiEnumeratingJoiner<A, B> getJoiner() {
        return joinerComber.mergedJoiner();
    }

    public @Nullable BiEnumeratingPredicate<Solution_, A, B> getFilter() {
        return joinerComber.mergedFiltering();
    }

    public Move<Solution_> buildMove(@Nullable A left, @Nullable B right) {
        return moveConstructor.apply(neighborhoodSession.getSolutionView(), left, right);
    }

}
