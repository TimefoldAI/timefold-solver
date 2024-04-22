package ai.timefold.solver.core.api.score.stream.common;

public interface ConnectedRange<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        extends Iterable<Interval_> {
    int getContainedRangeCount();

    boolean hasOverlap();

    int getMinimumOverlap();

    int getMaximumOverlap();

    Difference_ getLength();

    Point_ getStart();

    Point_ getEnd();
}
