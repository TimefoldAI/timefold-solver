package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;

/**
 * Points to a list variable position specified by an entity and an index.
 */
record DefaultLocationInList(@NonNull Object entity, int index) implements LocationInList {

    public DefaultLocationInList {
        Objects.requireNonNull(entity);
        if (index < 0) {
            throw new IllegalArgumentException("Impossible state: index (%d) not positive."
                    .formatted(index));
        }
    }

    @Override
    public @NonNull LocationInList ensureAssigned(@NonNull Supplier<String> messageSupplier) {
        return this;
    }

    @Override
    public boolean equals(Object element) {
        if (!(element instanceof DefaultLocationInList that)) {
            return false;
        }
        return index == that.index && entity == that.entity;
    }

    @Override
    public int hashCode() {
        var result = 1;
        result = 31 * result + (System.identityHashCode(entity));
        result = 31 * result + (Integer.hashCode(index));
        return result;
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
