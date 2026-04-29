package ai.timefold.solver.core.api.score.stream.bi;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link UniConstraintCollectorAccumulatedValue},
 * only for {@link BiConstraintCollector}.
 */
@NullMarked
public interface BiConstraintCollectorAccumulatedValue<A, B> {

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#add(Object)},
     * only for {@link BiConstraintCollector}
     */
    boolean add(@Nullable A a, @Nullable B b);

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#update(Object)},
     * only for {@link BiConstraintCollector}
     */
    boolean update(@Nullable A a, @Nullable B b);

    /**
     * As defined by {@link UniConstraintCollectorAccumulatedValue#remove()},
     * only for {@link BiConstraintCollector}
     */
    boolean remove();

}
