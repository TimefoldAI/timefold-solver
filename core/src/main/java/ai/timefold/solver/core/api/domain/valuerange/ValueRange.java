package ai.timefold.solver.core.api.domain.valuerange;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A ValueRange is a set of a values for a {@link PlanningVariable}.
 * These values might be stored in memory as a {@link Collection} (usually a {@link List} or {@link Set}),
 * but if the values are numbers, they can also be stored in memory by their bounds
 * to use less memory and provide more opportunities.
 * <p>
 * ValueRange is stateful.
 * Implementations must be immutable.
 * <p>
 * Prefer using {@link CountableValueRange}.
 * In a future version of Timefold Solver, uncountable value ranges will not be allowed,
 * and certain recently introduced features already do not support them.
 *
 * @see ValueRangeFactory
 * @see CountableValueRange
 */
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
    @NonNull
    Iterator<T> createRandomIterator(@NonNull Random workingRandom);

}
