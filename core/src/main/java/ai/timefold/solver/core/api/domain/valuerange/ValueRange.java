package ai.timefold.solver.core.api.domain.valuerange;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.primdouble.DoubleValueRange;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A ValueRange is a set of a values for a {@link PlanningVariable}.
 * These values might be stored in memory as a {@link Collection} (usually a {@link List} or {@link Set}),
 * but if the values are numbers, they can also be stored in memory by their bounds
 * to use less memory and provide more opportunities.
 * <p>
 * ValueRange is stateless, and its contents must not depend on any planning variables.
 * Implementations must be immutable.
 * <p>
 * Use {@link CountableValueRange} instead.
 * {@link ValueRange} only has a single non-countable implementation,
 * {@link DoubleValueRange}, which is deprecated and does not work any more.
 *
 * @see CountableValueRange
 * @see ValueRangeProvider
 * @see ValueRangeFactory
 */
@NullMarked
public interface ValueRange<T> {

    /**
     * In a {@link CountableValueRange}, this must be consistent with {@link CountableValueRange#getSize()}.
     *
     * @return true if the range is empty
     */
    boolean isEmpty();

    /**
     * @param value sometimes null
     * @return true if the ValueRange contains that value
     */
    boolean contains(@Nullable T value);

    /**
     * Select in random order, but without shuffling the elements.
     * Each element might be selected multiple times.
     * Scales well because it does not require caching.
     *
     * @param workingRandom the {@link Random} to use when any random number is needed,
     *        so runs are reproducible.
     */
    Iterator<T> createRandomIterator(Random workingRandom);

}
