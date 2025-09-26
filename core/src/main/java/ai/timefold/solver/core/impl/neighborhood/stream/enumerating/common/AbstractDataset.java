package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractDataset<Solution_, Tuple_ extends AbstractTuple> {

    private final EnumeratingStreamFactory<Solution_> enumeratingStreamFactory;
    private final AbstractEnumeratingStream<Solution_> parent;

    protected AbstractDataset(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractEnumeratingStream<Solution_> parent) {
        this.enumeratingStreamFactory = Objects.requireNonNull(enumeratingStreamFactory);
        this.parent = Objects.requireNonNull(parent);
    }

    public void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        parent.collectActiveEnumeratingStreams(enumeratingStreamSet);
    }

    public abstract AbstractDatasetInstance<Solution_, Tuple_> instantiate(int storeIndex);

    @Override
    public boolean equals(Object entity) {
        if (!(entity instanceof AbstractDataset<?, ?> dataset)) {
            return false;
        }
        return Objects.equals(enumeratingStreamFactory, dataset.enumeratingStreamFactory)
                && Objects.equals(parent, dataset.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enumeratingStreamFactory, parent);
    }

    @Override
    public String toString() {
        return "%s for %s".formatted(getClass().getSimpleName(), parent);
    }
}
