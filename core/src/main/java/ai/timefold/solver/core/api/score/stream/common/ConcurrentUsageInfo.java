package ai.timefold.solver.core.api.score.stream.common;

public interface ConcurrentUsageInfo<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>> {

    /**
     * @return never null, an iterable that iterates through the interval clusters
     *         contained in the collection in ascending order
     */
    Iterable<ConcurrentUsage<Interval_, Point_, Difference_>> getConcurrentUsages();

    /**
     * @return never null, an iterable that iterates through the breaks contained in
     *         the collection in ascending order
     */
    Iterable<Break<Point_, Difference_>> getBreaks();
}
