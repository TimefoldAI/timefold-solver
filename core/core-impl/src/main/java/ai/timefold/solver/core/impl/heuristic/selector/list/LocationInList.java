package ai.timefold.solver.core.impl.heuristic.selector.list;

import java.util.Objects;

/**
 * Points to a list variable position specified by an entity and an index.
 */
public record LocationInList(Object entity, int index) implements ElementLocation {

    public LocationInList {
        Objects.requireNonNull(entity);
        if (index < 0) {
            throw new IllegalArgumentException("Impossible state: index (%d) not positive."
                    .formatted(index));
        }
    }

}
