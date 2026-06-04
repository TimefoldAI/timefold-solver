package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link UniNeighborhoodsCollectorValueHandle}, only for {@link BiNeighborhoodsCollector}.
 *
 * @param <A> the type of the first fact in the source stream's tuple
 * @param <B> the type of the second fact in the source stream's tuple
 */
@NullMarked
public interface BiNeighborhoodsCollectorValueHandle<A, B> {

    void add(@Nullable A a, @Nullable B b);

    default void replaceWith(@Nullable A a, @Nullable B b) {
        remove();
        add(a, b);
    }

    void remove();

}
