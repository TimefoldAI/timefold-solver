package ai.timefold.solver.core.api.domain.metamodel;

import java.util.Objects;

/**
 * Points to a list variable position specified by an entity and an index.
 */
record DefaultLocationInList<Entity_>(Entity_ entity, int index) implements LocationInList<Entity_> {

    public DefaultLocationInList {
        Objects.requireNonNull(entity);
        if (index < 0) {
            throw new IllegalArgumentException("Impossible state: index (%d) not positive."
                    .formatted(index));
        }
    }

    @Override
    public String toString() {
        return entity + "[" + index + "]";
    }

}
