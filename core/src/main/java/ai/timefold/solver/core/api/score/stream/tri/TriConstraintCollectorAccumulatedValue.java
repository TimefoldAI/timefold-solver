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
    boolean add(@Nullable A a, @Nullable B b, @Nullable C c);

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#update(Object)},
     * only for {@link TriConstraintCollector}
     */
    boolean update(@Nullable A a, @Nullable B b, @Nullable C c);

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#remove()},
     * only for {@link TriConstraintCollector}
     */
    boolean remove();

}
