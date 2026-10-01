package ai.timefold.solver.core.api.score.stream.bi;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link UniConstraintCollectorValueHandle},
 * only for {@link BiConstraintCollector}.
 */
@NullMarked
public interface BiConstraintCollectorValueHandle<A, B> {

    /**
     * As defined by {@link UniConstraintCollectorValueHandle#add(Object)},
     * only for {@link BiConstraintCollector}
     */
    void add(@Nullable A a, @Nullable B b);

    /**
     * As defined by {@link UniConstraintCollectorValueHandle#replaceWith(Object)},
     * only for {@link BiConstraintCollector}
     */
    default void replaceWith(@Nullable A a, @Nullable B b) {
        remove();
        add(a, b);
    }

    /**
     * As defined by {@link UniConstraintCollectorValueHandle#remove()},
     * only for {@link BiConstraintCollector}
     */
    void remove();

}
