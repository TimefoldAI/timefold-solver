package ai.timefold.solver.core.api.score.stream.common;

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
     * @return never null, the number of ranges contained by this {@link ConnectedRange}.
     */
    int getContainedRangeCount();

    /**
     * True if this {@link ConnectedRange} has at least one pair of
     * ranges that overlaps each other, false otherwise.
     * 
     * @return never null, true iff there at least one pair of overlapping ranges in this {@link ConnectedRange}.
     */
    boolean hasOverlap();

    /**
     * Get the minimum number of overlapping ranges for any point contained by
     * this {@link ConnectedRange}.
     * 
     * @return never null, the minimum number of overlapping ranges for any point
     *         in this {@link ConnectedRange}.
     */
    int getMinimumOverlap();

    /**
     * Get the maximum number of overlapping ranges for any point contained by
     * this {@link ConnectedRange}.
     * 
     * @return never null, the maximum number of overlapping ranges for any point
     *         in this {@link ConnectedRange}.
     */
    int getMaximumOverlap();

    /**
     * Get the length of this {@link ConnectedRange}.
     * 
     * @return The difference between {@link #getEnd()} and {@link #getStart()}.
     */
    Difference_ getLength();

    /**
     * Gets the first start point represented by this {@link ConnectedRange}.
     * 
     * @return never null, the first start point represented by this {@link ConnectedRange}.
     */
    Point_ getStart();

    /**
     * Gets the last end point represented by this {@link ConnectedRange}.
     * 
     * @return never null, the last end point represented by this {@link ConnectedRange}.
     */
    Point_ getEnd();
}
