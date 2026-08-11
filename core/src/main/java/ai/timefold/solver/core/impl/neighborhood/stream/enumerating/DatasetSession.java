package ai.timefold.solver.core.impl.neighborhood.stream.enumerating;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.AbstractSession;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsBavetNodeNetwork;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.CachedBiDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.JustInTimeBiDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.BiLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.JustInTimeBiDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractRightDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniRightDatasetInstance;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DatasetSession<Solution_>
        extends AbstractSession<NeighborhoodsBavetNodeNetwork> {

    private final SolutionView<Solution_> solutionView;
    private final Map<AbstractDataset<Solution_>, AbstractDatasetInstance<Solution_, ?>> datasetInstanceMap =
            new IdentityHashMap<>();

    DatasetSession(NeighborhoodsBavetNodeNetwork nodeNetwork, SolutionView<Solution_> solutionView) {
        super(nodeNetwork);
        this.solutionView = Objects.requireNonNull(solutionView);
    }

    public void registerDatasetInstance(AbstractDataset<Solution_> dataset,
            AbstractDatasetInstance<Solution_, ?> datasetInstance) {
        var oldDatasetInstance = datasetInstanceMap.put(dataset, datasetInstance);
        if (oldDatasetInstance != null) {
            throw new IllegalStateException("The dataset (%s) has already been registered with session (%s)."
                    .formatted(dataset, this));
        }
    }

    @SuppressWarnings("unchecked")
    public <Instance_> Instance_ getInstance(AbstractDataset<Solution_> dataset) {
        return (Instance_) Objects.requireNonNull(datasetInstanceMap.get(dataset));
    }

    public <A> UniDatasetInstance<A> getInstance(UniDataset<Solution_, A> dataset) {
        var uniLeftDataset = (UniLeftDataset<Solution_, A>) dataset;
        return new DefaultUniDatasetInstance<>(
                this.<AbstractLeftDatasetInstance<Solution_, UniTuple<A>>> getInstance(uniLeftDataset));
    }

    public <A, B> BiDatasetInstance<A, B> getInstance(BiDataset<Solution_, A, B> dataset) {
        if (dataset instanceof JustInTimeBiDataset<Solution_, A, B>(var leftDataset, var rightDataset)) {
            var leftInstance = this.<AbstractLeftDatasetInstance<Solution_, UniTuple<A>>> getInstance(leftDataset);
            var rightInstance = (UniRightDatasetInstance<Solution_, A, B>) this
                    .<AbstractRightDatasetInstance<Solution_, B>> getInstance(rightDataset);
            return new JustInTimeBiDatasetInstance<>(leftInstance, rightInstance, solutionView);
        }
        var biLeftDataset = (BiLeftDataset<Solution_, A, B>) dataset;
        return new CachedBiDatasetInstance<>(
                this.<AbstractLeftDatasetInstance<Solution_, BiTuple<A, B>>> getInstance(biLeftDataset));
    }

}
