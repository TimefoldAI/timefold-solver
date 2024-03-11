package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.CachingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.CachingMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;

/**
 * A {@link ValueSelector} that caches the result of its child {@link ValueSelector}.
 * <p>
 * Keep this code in sync with {@link CachingEntitySelector} and {@link CachingMoveSelector}.
 */
public final class CachingValueSelector<Solution_>
        extends AbstractCachingValueSelector<Solution_>
        implements EntityIndependentValueSelector<Solution_> {

    protected final boolean randomSelection;

    public CachingValueSelector(EntityIndependentValueSelector<Solution_> childValueSelector,
            SelectionCacheType cacheType, boolean randomSelection) {
        super(childValueSelector, cacheType);
        this.randomSelection = randomSelection;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isNeverEnding() {
        // CachedListRandomIterator is neverEnding
        return randomSelection;
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        return iterator();
    }

    @Override
    public Iterator<Object> iterator() {
        if (!randomSelection) {
            return cachedValueList.iterator();
        } else {
            return new CachedListRandomIterator<>(cachedValueList, workingRandom);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        if (!super.equals(other))
            return false;
        CachingValueSelector<?> that = (CachingValueSelector<?>) other;
        return randomSelection == that.randomSelection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), randomSelection);
    }

    @Override
    public String toString() {
        return "Caching(" + childValueSelector + ")";
    }

}
