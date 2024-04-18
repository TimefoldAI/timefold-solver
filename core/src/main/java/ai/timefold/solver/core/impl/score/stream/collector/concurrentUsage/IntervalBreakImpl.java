package ai.timefold.solver.core.impl.score.stream.collector.concurrentUsage;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.ConcurrentUsage;

final class IntervalBreakImpl<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements Break<Point_, Difference_> {
    private ConcurrentUsage<Interval_, Point_, Difference_> previousCluster;
    private ConcurrentUsage<Interval_, Point_, Difference_> nextCluster;
    private Difference_ length;

    IntervalBreakImpl(ConcurrentUsage<Interval_, Point_, Difference_> previousCluster,
            ConcurrentUsage<Interval_, Point_, Difference_> nextCluster, Difference_ length) {
        this.previousCluster = previousCluster;
        this.nextCluster = nextCluster;
        this.length = length;
    }

    public ConcurrentUsage<Interval_, Point_, Difference_> getPreviousConcurrentUsage() {
        return previousCluster;
    }

    public ConcurrentUsage<Interval_, Point_, Difference_> getNextConcurrentUsage() {
        return nextCluster;
    }

    @Override
    public boolean isFirst() {
        return previousCluster == null;
    }

    @Override
    public boolean isLast() {
        return nextCluster == null;
    }

    @Override
    public Point_ getPreviousSequenceEnd() {
        return previousCluster.getEnd();
    }

    @Override
    public Point_ getNextSequenceStart() {
        return nextCluster.getStart();
    }

    @Override
    public Difference_ getLength() {
        return length;
    }

    void setPreviousCluster(ConcurrentUsage<Interval_, Point_, Difference_> previousCluster) {
        this.previousCluster = previousCluster;
    }

    void setNextCluster(ConcurrentUsage<Interval_, Point_, Difference_> nextCluster) {
        this.nextCluster = nextCluster;
    }

    void setLength(Difference_ length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return "IntervalBreak{" +
                "previousCluster=" + previousCluster +
                ", nextCluster=" + nextCluster +
                ", length=" + length +
                '}';
    }
}
