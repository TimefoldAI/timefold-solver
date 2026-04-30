package ai.timefold.solver.core.api.score.stream.tri;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link UniConstraintCollectorAccumulatedValue},
 * only for {@link TriConstraintCollector}.
 */
@NullMarked
public interface TriConstraintCollectorAccumulatedValue<A, B, C> {

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#add(Object)},
     * only for {@link TriConstraintCollector}
     */
    void add(@Nullable A a, @Nullable B b, @Nullable C c);

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#update(Object)},
     * only for {@link TriConstraintCollector}
     */
    default void update(@Nullable A a, @Nullable B b, @Nullable C c) {
        remove();
        add(a, b, c);
    }

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#remove()},
     * only for {@link TriConstraintCollector}
     */
    void remove();

}
