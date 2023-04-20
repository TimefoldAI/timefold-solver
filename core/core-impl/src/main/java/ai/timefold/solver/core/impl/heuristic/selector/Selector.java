package ai.timefold.solver.core.impl.heuristic.selector;

import java.util.Iterator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;

/**
 * General interface for {@link MoveSelector}, {@link EntitySelector} and {@link ValueSelector}
 * which generates {@link Move}s or parts of them.
 */
public interface Selector<Solution_> extends PhaseLifecycleListener<Solution_> {

    /**
     * If false, then {@link #isNeverEnding()} is true.
     *
     * @return true if all the {@link ValueRange}s are countable
     *         (for example a double value range between 1.2 and 1.4 is not countable)
     */
    boolean isCountable();

    /**
     * Is true if {@link #isCountable()} is false
     * or if this selector is in random order (for most cases).
     * Is never true when this selector is in shuffled order (which is less scalable but more exact).
     *
     * @return true if the {@link Iterator#hasNext()} of the {@link Iterator} created by {@link Iterable#iterator()}
     *         never returns false (except when it's empty).
     */
    boolean isNeverEnding();

    /**
     * Unless this selector itself caches, this returns {@link SelectionCacheType#JUST_IN_TIME},
     * even if a selector child caches.
     *
     * @return never null
     */
    SelectionCacheType getCacheType();

}
