package ai.timefold.solver.core.api.score.stream.common;

public interface ConnectedRangeChain<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>> {

    /**
     * @return never null, an iterable that iterates through the connected ranges
     *         contained in the collection in ascending order of their start points
     */
    Iterable<ConnectedRange<Interval_, Point_, Difference_>> getConnectedRanges();

    /**
     * @return never null, an iterable that iterates through the breaks contained in
     *         the collection in ascending order of their start points
     */
    Iterable<RangeGap<Point_, Difference_>> getGaps();
}
