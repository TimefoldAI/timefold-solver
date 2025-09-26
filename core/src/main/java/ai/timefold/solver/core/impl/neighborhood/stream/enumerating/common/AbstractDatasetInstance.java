package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractDatasetInstance<Solution_, Tuple_ extends AbstractTuple>
        implements TupleLifecycle<Tuple_> {

    private final AbstractDataset<Solution_, Tuple_> parent;
    protected final int inputStoreIndex;

    protected AbstractDatasetInstance(AbstractDataset<Solution_, Tuple_> parent, int inputStoreIndex) {
        this.parent = Objects.requireNonNull(parent);
        this.inputStoreIndex = inputStoreIndex;
    }

    public AbstractDataset<Solution_, Tuple_> getParent() {
        return parent;
    }

    public abstract Iterator<Tuple_> iterator();

    public abstract Iterator<Tuple_> iterator(Random workingRandom);

}
