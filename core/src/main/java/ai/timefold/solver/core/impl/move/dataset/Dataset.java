package ai.timefold.solver.core.impl.move.dataset;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

public final class Dataset<Solution_, Tuple_ extends AbstractTuple> {

    private final DefaultDatasetFactory<Solution_> defaultDatasetFactory;
    private final AbstractDataStream<Solution_> parent;

    public Dataset(DefaultDatasetFactory<Solution_> defaultDatasetFactory,
            AbstractDataStream<Solution_> parent) {
        this.defaultDatasetFactory = Objects.requireNonNull(defaultDatasetFactory);
        this.parent = Objects.requireNonNull(parent);
    }

    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> dataStreamSet) {
        parent.collectActiveDataStreams(dataStreamSet);
    }

    public DatasetInstance<Solution_, Tuple_> instantiate(int inputStoreIndex) {
        return new DatasetInstance<>(this, inputStoreIndex);
    }

    @Override
    public boolean equals(Object entity) {
        if (!(entity instanceof Dataset<?, ?> dataset)) {
            return false;
        }
        return Objects.equals(defaultDatasetFactory, dataset.defaultDatasetFactory) && Objects.equals(parent, dataset.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultDatasetFactory, parent);
    }
}
