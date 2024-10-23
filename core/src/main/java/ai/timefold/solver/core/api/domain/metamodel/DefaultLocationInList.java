package ai.timefold.solver.core.api.domain.metamodel;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Points to a list variable position specified by an entity and an index.
 */
record DefaultLocationInList(Object entity, int index) implements LocationInList {

    public DefaultLocationInList {
        Objects.requireNonNull(entity);
        if (index < 0) {
            throw new IllegalArgumentException("Impossible state: index (%d) not positive."
                    .formatted(index));
        }
    }

    @Override
    public LocationInList ensureAssigned(Supplier<String> messageSupplier) {
        return this;
    }

    @Override
    public String toString() {
        return entity + "[" + index + "]";
    }

    @Override
    public int compareTo(LocationInList other) {
        return Integer.compare(index, other.index());
    }
}
