package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractDatasetInstance<Solution_, Tuple_ extends Tuple>
        implements TupleLifecycle<Tuple_> {

    private final AbstractDataset<Solution_> parent;
    protected final int entryStoreIndex;

    protected AbstractDatasetInstance(AbstractDataset<Solution_> parent, int entryStoreIndex) {
        this.parent = Objects.requireNonNull(parent);
        this.entryStoreIndex = entryStoreIndex;
    }

    @Override
    public void initialize(boolean upstreamCanProduceTuples) {
        // TODO implement
    }

    @Override
    public boolean isActive() {
        return true; // TODO implement
    }

    public AbstractDataset<Solution_> getParent() {
        return parent;
    }

}
