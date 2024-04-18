package ai.timefold.solver.core.api.score.stream.common;

public interface ConcurrentUsage<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        extends Iterable<Interval_> {
    int size();

    boolean hasOverlap();

    int getMinimumConcurrentUsage();

    int getMaximumConcurrentUsage();

    Difference_ getLength();

    Point_ getStart();

    Point_ getEnd();
}
