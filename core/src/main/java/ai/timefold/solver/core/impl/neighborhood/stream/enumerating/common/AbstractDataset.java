package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractDataset<Solution_> {

    protected final AbstractEnumeratingStream<Solution_> parent;

    protected AbstractDataset(AbstractEnumeratingStream<Solution_> parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    public void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        parent.collectActiveEnumeratingStreams(enumeratingStreamSet);
    }

    @Override
    public abstract boolean equals(Object o); // So that subclasses must implement it.

    @Override
    public abstract int hashCode(); // So that subclasses must implement it.

    @Override
    public String toString() {
        return "%s for %s".formatted(getClass().getSimpleName(), parent);
    }
}
