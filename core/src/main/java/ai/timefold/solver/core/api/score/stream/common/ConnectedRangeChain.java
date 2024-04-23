package ai.timefold.solver.core.api.score.stream.common;

/**
 * Contains info regarding connected ranges and gaps for a collection of ranges.
 *
 * @param <Interval_> The type of range in the collection.
 * @param <Point_> The type of the start and end points for each range.
 * @param <Difference_> The type of difference between start and end points.
 */
public interface ConnectedRangeChain<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>> {

    /**
     * @return never null, an iterable that iterates through the connected ranges
     *         contained in the collection in ascending order of their start points
     */
    Iterable<ConnectedRange<Interval_, Point_, Difference_>> getConnectedRanges();

    /**
     * @return never null, an iterable that iterates through the gaps contained in
     *         the collection in ascending order of their start points
     */
    Iterable<RangeGap<Point_, Difference_>> getGaps();
}
