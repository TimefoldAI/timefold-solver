package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import ai.timefold.solver.core.impl.neighborhood.stream.dataset.JustInTimeBiDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniRightDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link BiDataset} produced by {@link UniLeftDataset#join}:
 * the join is not materialized in Bavet but instead computed just in time,
 * inside the {@link JustInTimeBiDatasetInstance} it resolves to.
 */
@NullMarked
public record JustInTimeBiDataset<Solution_, A, B>(UniLeftDataset<Solution_, A> leftDataset,
        UniRightDataset<Solution_, A, B> rightDataset) implements BiDataset<Solution_, A, B> {

}
