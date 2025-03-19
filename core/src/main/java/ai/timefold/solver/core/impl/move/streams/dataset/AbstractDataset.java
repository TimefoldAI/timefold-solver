package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

public abstract class AbstractDataset<Solution_, Tuple_ extends AbstractTuple> {

    private final DataStreamFactory<Solution_> dataStreamFactory;
    private final AbstractDataStream<Solution_> parent;

    protected AbstractDataset(DataStreamFactory<Solution_> dataStreamFactory, AbstractDataStream<Solution_> parent) {
        this.dataStreamFactory = Objects.requireNonNull(dataStreamFactory);
        this.parent = Objects.requireNonNull(parent);
    }

    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> dataStreamSet) {
        parent.collectActiveDataStreams(dataStreamSet);
    }

    public DatasetInstance<Solution_, Tuple_> instantiate(int storeIndex) {
        return new DatasetInstance<>(this, storeIndex);
    }

    @Override
    public boolean equals(Object entity) {
        if (!(entity instanceof AbstractDataset<?, ?> dataset)) {
            return false;
        }
        return Objects.equals(dataStreamFactory, dataset.dataStreamFactory) && Objects.equals(parent, dataset.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataStreamFactory, parent);
    }

    @Override
    public String toString() {
        return "%s for %s".formatted(getClass().getSimpleName(), parent);
    }
}
