package ai.timefold.solver.core.api.score.stream.common;

import java.util.Collection;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.jspecify.annotations.NonNull;

/**
 * Represents a collection of ranges that are connected, meaning
 * the union of all the ranges results in the range
 * [{@link #getStart()}, {@link #getEnd()}) without gaps.
 *
 * @param <Range_> The type of range in the collection.
 * @param <Point_> The type of the start and end points for each range.
 * @param <Difference_> The type of difference between start and end points.
 */
public interface ConnectedRange<Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        extends Iterable<Range_> {
    /**
     * Get the number of ranges contained by this {@link ConnectedRange}.
     *
     * @return the number of ranges contained by this {@link ConnectedRange}.
     */
    int getContainedRangeCount();

    /**
     * True if this {@link ConnectedRange} has at least one pair of
     * ranges that overlaps each other, false otherwise.
     *
     * @return true iff there at least one pair of overlapping ranges in this {@link ConnectedRange}.
     */
    boolean hasOverlap();

    /**
     * Get the minimum number of overlapping ranges for any point contained by
     * this {@link ConnectedRange}.
     *
     * @return the minimum number of overlapping ranges for any point
     *         in this {@link ConnectedRange}.
     */
    int getMinimumOverlap();

    /**
     * Get the maximum number of overlapping ranges for any point contained by
     * this {@link ConnectedRange}.
     *
     * @return the maximum number of overlapping ranges for any point
     *         in this {@link ConnectedRange}.
     */
    int getMaximumOverlap();

    /**
     * Get the maximum sum of a function amongst distinct ranges of overlapping values
     * amongst all points contained by this {@link ConnectedRange}.
     *
     * @return get the maximum sum of a function amongst distinct ranges of overlapping values
     *         for any point contained by this {@link ConnectedRange}.
     */
    int getMaximumValue(ToIntFunction<? super Range_> functionSupplier);

    /**
     * Get the maximum sum of a function amongst distinct ranges of overlapping values
     * amongst all points contained by this {@link ConnectedRange}. This method allows you to use
     * a function that takes all active ranges as an input. Use {@link ::getMaximumValue} if possible
     * for efficiency.
     *
     * @return get the maximum sum of a function amongst distinct ranges of overlapping values
     *         for any point contained by this {@link ConnectedRange}.
     */
    int getMaximumValueForDistinctRanges(ToIntBiFunction<Collection<? super Range_>, Difference_> functionSupplier);

    /**
     * Get the length of this {@link ConnectedRange}.
     *
     * @return The difference between {@link #getEnd()} and {@link #getStart()}.
     */
    @NonNull
    Difference_ getLength();

    /**
     * Gets the first start point represented by this {@link ConnectedRange}.
     *
     * @return never null, the first start point represented by this {@link ConnectedRange}.
     */
    @NonNull
    Point_ getStart();

    /**
     * Gets the last end point represented by this {@link ConnectedRange}.
     *
     * @return the last end point represented by this {@link ConnectedRange}.
     */
    @NonNull
    Point_ getEnd();
}
