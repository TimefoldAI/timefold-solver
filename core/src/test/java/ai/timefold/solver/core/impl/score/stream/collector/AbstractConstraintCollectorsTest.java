package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.connected_ranges.ConnectedRangeTracker;
import ai.timefold.solver.core.impl.score.stream.collector.consecutive.ConsecutiveSetTree;

import org.junit.jupiter.api.Test;

/**
 * Prescribes tests that must be implemented for every cardinality of a constraint collector.
 */
public abstract class AbstractConstraintCollectorsTest {

    @Test
    public abstract void count();

    @Test
    public abstract void countLong();

    @Test
    public abstract void countDistinct();

    @Test
    public abstract void countDistinctLong();

    @Test
    public abstract void sum();

    @Test
    public abstract void sumLong();

    @Test
    public abstract void sumBigDecimal();

    @Test
    public abstract void sumBigInteger();

    @Test
    public abstract void sumDuration();

    @Test
    public abstract void sumPeriod();

    @Test
    public abstract void minComparable();

    @Test
    public abstract void minNotComparable();

    @Test
    public abstract void maxComparable();

    @Test
    public abstract void maxNotComparable();

    @Test
    public abstract void average();

    @Test
    public abstract void averageLong();

    @Test
    public abstract void averageBigDecimal();

    @Test
    public abstract void averageBigInteger();

    @Test
    public abstract void averageDuration();

    @Test
    public abstract void toSet();

    @Test
    public abstract void toSortedSet();

    @Test
    public abstract void toList();

    @Test
    public abstract void toMap();

    @Test
    public abstract void toMapMerged();

    @Test
    public abstract void toSortedMap();

    @Test
    public abstract void toSortedMapMerged();

    @Test
    public abstract void conditionally();

    @Test
    public abstract void compose2();

    @Test
    public abstract void compose3();

    @Test
    public abstract void compose4();

    @Test
    public abstract void toConsecutiveSequences();

    @Test
    public abstract void consecutiveUsage();

    @Test
    public abstract void consecutiveUsageDynamic();

    @Test
    public abstract void loadBalance();

    @Test
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
