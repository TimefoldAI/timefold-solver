package ai.timefold.solver.core.api.score.stream.quad;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link UniConstraintCollectorAccumulatedValue},
 * only for {@link QuadConstraintCollector}.
 */
@NullMarked
public interface QuadConstraintCollectorAccumulatedValue<A, B, C, D> {

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#add(Object)},
     * only for {@link QuadConstraintCollector}
     */
    boolean add(@Nullable A a, @Nullable B b, @Nullable C c, @Nullable D d);

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#update(Object)},
     * only for {@link QuadConstraintCollector}
     */
    boolean update(@Nullable A a, @Nullable B b, @Nullable C c, @Nullable D d);

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#remove()},
     * only for {@link QuadConstraintCollector}
     */
    boolean remove();

}
