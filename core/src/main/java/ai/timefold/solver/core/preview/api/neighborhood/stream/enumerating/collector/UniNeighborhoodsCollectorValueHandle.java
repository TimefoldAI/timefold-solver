package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a handle for a single value in a single {@link UniNeighborhoodsCollectorAccumulator} group.
 * The handle is obtained from {@link UniNeighborhoodsCollectorAccumulator#intoGroup} when a new value enters the group,
 * and is used to update or remove that value.
 *
 * @param <A> the type of the only fact in the source stream's tuple
 */
@NullMarked
public interface UniNeighborhoodsCollectorValueHandle<A> {

    void add(@Nullable A a);

    default void replaceWith(@Nullable A a) {
        remove();
        add(a);
    }

    void remove();

}
