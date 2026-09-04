package ai.timefold.solver.core.impl.move.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.NullAllowingValueRange;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The distinct {@link ValueRange}s of one {@link Sample}'s members, for one variable: everything a sample-drawing move
 * iterator needs to find a destination legal for every member, without ever building a global candidate pool or calling
 * {@code isValueInRange} once per member.
 * <p>
 * Every {@link ValueRange} implementation has a content-based {@code equals}/{@code hashCode},
 * so the distinct set - and the smallest range within it - are both found in one O(sample size) pass;
 * usually the set holds a single, shared range instance, since {@code ValueRangeState} already deduplicates equal ranges
 * to one cached instance.
 * <p>
 * Two instances are {@link #equals equal} when they hold the same distinct ranges, regardless of order -
 * used to remember a proven-empty verdict across redraws of the same sample under a deterministic {@link Sampler}.
 *
 * @param <Value_> the variable's value type
 */
@NullMarked
public record SampleValueRanges<Value_>(Set<ValueRange<Value_>> distinctRangeSet, ValueRange<Value_> smallestRange) {

    private static final long MAX_SAFE_RANGE_SIZE = Long.MAX_VALUE / FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;

    /**
     * @return the sample members' distinct {@link ValueRange}s for {@code variableMetaModel}.
     *         Ranges may still be {@link NullAllowingValueRange}-wrapped; {@code null} is never a candidate destination
     *         out of {@link #findTarget}/{@link #pickExactly} regardless,
     *         since {@link #containsInEvery} rejects it directly - it would otherwise collide with their "not found" signal.
     *         A caller that wants a null destination decides on it separately with {@link #rollNull},
     *         before calling either method.
     */
    public static <Solution_, Entity_, Value_> SampleValueRanges<Value_> of(Sample<Entity_> sample,
            GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, SolutionView<Solution_> solutionView) {
        if (variableMetaModel.isValueRangeOnSolution()) {
            // Every member provably shares one range instance (the range doesn't depend on the entity
            // argument at all), so one lookup answers for the whole sample - no dedup, no unwrap needed.
            var range = solutionView.getValueRange(variableMetaModel, null);
            return new SampleValueRanges<>(Set.of(range), range);
        }
        // Tracks distinct ranges without a hash table until a genuinely second one shows up:
        // ValueRangeState already deduplicates equal ranges to one shared instance,
        // so the common case is exactly one distinct range,
        // and a HashSet's backing table would be pure overhead for that.
        ValueRange<Value_> firstRange = null;
        List<ValueRange<Value_>> distinctRangeList = null;
        for (var entity : sample) {
            var range = solutionView.getValueRange(variableMetaModel, entity);
            if (firstRange == null) {
                firstRange = range;
            } else if (distinctRangeList == null) {
                if (!range.equals(firstRange)) {
                    // Size 4 is arbitrary, but it's a good guess for the common case of 1 or 2 distinct ranges,
                    // while giving the backing array a chance to avoid resizing.
                    distinctRangeList = new ArrayList<>(4);
                    distinctRangeList.add(firstRange);
                    distinctRangeList.add(range);
                }
            } else if (!distinctRangeList.contains(range)) {
                distinctRangeList.add(range);
            }
        }
        return of(distinctRangeList == null ? Set.of(firstRange) : Set.copyOf(distinctRangeList));
    }

    /**
     * @return an instance over an already-known set of distinct ranges; exposed mainly so the reservoir-sampling fallback
     *         ({@link #pickExactly}) can be bias-tested directly against plain {@link ValueRange} fixtures,
     *         without needing a {@link Sample} or a solution.
     */
    public static <Value_> SampleValueRanges<Value_> of(Set<ValueRange<Value_>> distinctRangeSet) {
        ValueRange<Value_> smallestRange = null;
        for (var range : distinctRangeSet) {
            if (smallestRange == null || range.getSize() < smallestRange.getSize()) {
                smallestRange = range;
            }
        }
        return new SampleValueRanges<>(distinctRangeSet, Objects.requireNonNull(smallestRange));
    }

    /**
     * @return {@code range}'s size, excluding the {@code null} pseudo-value a {@link NullAllowingValueRange} wrapper
     *         would otherwise add to it - {@code null} is decided separately by {@link #rollNull},
     *         never counted as one of the range's own values.
     */
    private static long unwrappedSize(ValueRange<?> range) {
        return range instanceof NullAllowingValueRange<?> nullAllowing
                ? nullAllowing.getChildValueRange().getSize()
                : range.getSize();
    }

    public static long bailOutSizeOf(ValueRange<?> range) {
        // Clamped since a range's {@link ValueRange#getSize()} can be large enough that multiplying it here would
        // overflow a {@code long} negative - which {@link FilteringIterator} reads as "bail-out disabled",
        // turning {@code hasNext()} into an infinite loop.
        return Math.min(unwrappedSize(range), MAX_SAFE_RANGE_SIZE) * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
    }

    /**
     * Finds one value legal for every distinct range and not equal to {@code excludedValue}
     * (pass {@code null} when there is nothing to exclude).
     * <p>
     * Samples {@link #smallestRange} first - for the common single-range case this is the whole cost,
     * since every candidate it offers is already legal for every member and the first one is always accepted.
     * Only when sampling bails out does this fall back to {@link #pickExactly},
     * which proves the answer exactly instead of guessing again.
     *
     * @return a value legal for every distinct range, or {@code null} if none exists
     */
    public @Nullable Value_ findTarget(RandomGenerator random, @Nullable Value_ excludedValue) {
        var bailOutSize = bailOutSizeOf(smallestRange);
        var sampledCandidates = new FilteringIterator<>(smallestRange.createRandomIterator(random),
                candidate -> !Objects.equals(candidate, excludedValue) && containsInEvery(candidate), bailOutSize);
        if (sampledCandidates.hasNext()) {
            return sampledCandidates.next();
        }
        return pickExactly(random, excludedValue);
    }

    /**
     * The exhaustive fallback: proves whether a destination legal for every distinct range exists,
     * without ever materializing the intersection.
     * One pass over the smallest range, reservoir-sampling a single uniform pick among the values admitted by every
     * distinct range and not equal to {@code excludedValue} -
     * so the result is either a uniform draw from the true intersection, or a proof that no legal destination exists.
     *
     * @return {@code null} if no member of the smallest range is admitted by every distinct range
     *         (after excluding {@code excludedValue}), meaning the intersection is empty
     */
    public @Nullable Value_ pickExactly(RandomGenerator random, @Nullable Value_ excludedValue) {
        Value_ chosen = null;
        var admittedCount = 0;
        var iterator = smallestRange.createOriginalIterator();
        while (iterator.hasNext()) {
            var candidate = iterator.next();
            if (Objects.equals(candidate, excludedValue) || !containsInEvery(candidate)) {
                continue;
            }
            admittedCount++;
            if (random.nextInt(admittedCount) == 0) {
                chosen = candidate;
            }
        }
        return chosen;
    }

    /**
     * @return {@code true} with probability {@code 1/(size+1)},
     *         where {@code size} is {@link #smallestRange}'s clamped size - the same probability a
     *         {@link NullAllowingValueRange} wrapper on {@link #smallestRange} would have given {@code null} as a candidate,
     *         without ever handing {@code null} to {@link #findTarget}/{@link #pickExactly},
     *         where it would collide with their "not found" signal.
     *         Null is legal for every member whenever the variable allows unassigned values,
     *         so no range intersection is needed here - only the coin flip.
     */
    public boolean rollNull(RandomGenerator random) {
        var size = Math.min(unwrappedSize(smallestRange), MAX_SAFE_RANGE_SIZE);
        return RandomUtils.nextLong(random, size + 1L) == 0L;
    }

    /**
     * @return true if {@code value} is legal for every distinct range in this instance; {@code false} for {@code null},
     *         decided separately by {@link #rollNull}
     */
    public boolean containsInEvery(@Nullable Value_ value) {
        if (value == null) {
            return false;
        }
        for (var range : distinctRangeSet) {
            if (!range.contains(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SampleValueRanges<?> other && distinctRangeSet.equals(other.distinctRangeSet);
    }

    @Override
    public int hashCode() {
        return distinctRangeSet.hashCode();
    }

}
