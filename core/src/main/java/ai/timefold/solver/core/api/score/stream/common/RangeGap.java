package ai.timefold.solver.core.api.score.stream.common;

/**
 * A {@link RangeGap} is a gap between two consecutive {@link ConnectedRange}s.
 * For instance, the list [(1,3),(2,4),(3,5),(7,8)] has a grap of length 2 between 5 and 7.
 *
 * @param <Point_> The type for the ranges' start and end points
 * @param <Difference_> The type of difference between values in the sequence
 */
public interface RangeGap<Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>> {
    /**
     * Return the end of the {@link ConnectedRange} before this gap.
     * For the gap between 6 and 10, this will return 6.
     *
     * @return never null, the item this gap is directly after
     */
    Point_ getPreviousRangeEnd();

    /**
     * Return the start of the {@link ConnectedRange} after this gap.
     * For the gap between 6 and 10, this will return 10.
     *
     * @return never null, the item this gap is directly before
     */
    Point_ getNextRangeStart();

    /**
     * Return the length of the break, which is the difference
     * between {@link #getNextRangeStart()} and {@link #getPreviousRangeEnd()}.
     * For the gap between 6 and 10, this will return 4.
     *
     * @return never null, the length of this break
     */
    Difference_ getLength();
}
