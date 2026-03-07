package ai.timefold.solver.core.api.score.stream.common;

import java.util.Collection;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.jspecify.annotations.NonNull;

/**
 * Contains info regarding {@link ConnectedRange}s and {@link RangeGap}s for a collection of ranges.
 *
 * @param <Range_> The type of range in the collection.
 * @param <Point_> The type of the start and end points for each range.
 * @param <Difference_> The type of difference between start and end points.
 */
public interface ConnectedRangeChain<Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>> {

    /**
     * @return an iterable that iterates through the {@link ConnectedRange}s
     *         contained in the collection in ascending order of their start points
     */
    @NonNull
    Iterable<ConnectedRange<Range_, Point_, Difference_>> getConnectedRanges();

    /**
     * @return an iterable that iterates through the {@link RangeGap}s contained in
     *         the collection in ascending order of their start points
     */
    @NonNull
    Iterable<RangeGap<Point_, Difference_>> getGaps();

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
}
