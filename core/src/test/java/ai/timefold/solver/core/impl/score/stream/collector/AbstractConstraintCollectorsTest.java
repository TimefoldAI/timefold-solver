package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.connected_ranges.ConnectedRangeTracker;
import ai.timefold.solver.core.impl.score.stream.collector.consecutive.ConsecutiveSetTree;

/**
 * Prescribes tests that must be implemented for every cardinality of a constraint collector.
 */
public abstract class AbstractConstraintCollectorsTest {

    public abstract void count();

    public abstract void countLong();

    public abstract void countDistinct();

    public abstract void countDistinctLong();

    public abstract void sum();

    public abstract void sumLong();

    public abstract void sumBigDecimal();

    public abstract void sumBigInteger();

    public abstract void sumDuration();

    public abstract void sumPeriod();

    public abstract void minComparable();

    public abstract void minNotComparable();

    public abstract void maxComparable();

    public abstract void maxNotComparable();

    public abstract void average();

    public abstract void averageLong();

    public abstract void averageBigDecimal();

    public abstract void averageBigInteger();

    public abstract void averageDuration();

    public abstract void toSet();

    public abstract void toSortedSet();

    public abstract void toList();

    public abstract void toMap();

    public abstract void toMapMerged();

    public abstract void toSortedMap();

    public abstract void toSortedMapMerged();

    public abstract void conditionally();

    public abstract void compose2();

    public abstract void compose3();

    public abstract void compose4();

    public abstract void toConsecutiveSequences();

    public abstract void consecutiveUsage();

    public abstract void consecutiveUsageDynamic();

    public abstract void loadBalance();

    public abstract void collectAndThen();

    protected static SequenceChain<Integer, Integer> buildSequenceChain(Integer... data) {
        return Arrays.stream(data).collect(
                () -> new ConsecutiveSetTree<Integer, Integer, Integer>((a, b) -> b - a, Integer::sum, 1, 0),
                (tree, datum) -> tree.add(datum, datum),
                (a1, b1) -> {
                    throw new UnsupportedOperationException();
                });
    }

    protected ConnectedRangeChain<Interval, Integer, Integer> buildConsecutiveUsage(Interval... data) {
        return Arrays.stream(data).collect(
                () -> new ConnectedRangeTracker<>(Interval::start, Interval::end, (a, b) -> b - a),
                (tree, datum) -> tree.add(tree.getRange(datum)),
                (a, b) -> {
                    throw new UnsupportedOperationException();
                }).getConnectedRangeChain();
    }

    protected ConnectedRangeChain<DynamicInterval, Integer, Integer> buildDynamicConsecutiveUsage(DynamicInterval... data) {
        return Arrays.stream(data).collect(
                () -> new ConnectedRangeTracker<>(DynamicInterval::getStart, DynamicInterval::getEnd, (a, b) -> b - a),
                (tree, datum) -> tree.add(tree.getRange(datum)),
                (a, b) -> {
                    throw new UnsupportedOperationException();
                }).getConnectedRangeChain();
    }

    public record Interval(int start, int end) {

    }

    public static final class DynamicInterval {
        int start;

        public DynamicInterval(int start) {
            this.start = start;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return start + 10;
        }

        public void setStart(int start) {
            this.start = start;
        }

        @Override
        public String toString() {
            return "DynamicInterval(%d, %d)".formatted(getStart(), getEnd());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof DynamicInterval that))
                return false;
            return start == that.start;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(start);
        }
    }

}
