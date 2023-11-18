package ai.timefold.solver.examples.common.experimental;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.function.BiConsumer;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.examples.common.experimental.impl.ConsecutiveIntervalInfoImpl;
import ai.timefold.solver.examples.common.experimental.impl.IntervalTree;

import org.junit.jupiter.api.Test;

class ExperimentalConstraintCollectorsTest {

    @Test
    void consecutiveInterval() {
        // Do a basic test w/o edge cases; edge cases are covered in ConsecutiveSetTreeTest
        var collector =
                ExperimentalConstraintCollectors.consecutiveIntervals(Interval::start, Interval::end, (a, b) -> b - a);
        var container = collector.supplier().get();
        // Add first value, sequence is [(1,3)]
        Interval firstValue = new Interval(1, 3);
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, consecutiveIntervalData(firstValue));
        // Add second value, sequence is [(1,3),(2,4)]
        Interval secondValue = new Interval(2, 4);
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, consecutiveIntervalData(firstValue, secondValue));
        // Add third value, same as the second. Sequence is [{1,1},2}]
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, consecutiveIntervalData(firstValue, secondValue, secondValue));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, consecutiveIntervalData(firstValue, secondValue));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, consecutiveIntervalData(firstValue));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, consecutiveIntervalData());
    }

    private ConsecutiveIntervalInfoImpl<Interval, Integer, Integer> consecutiveIntervalData(Interval... data) {
        return Arrays.stream(data).collect(
                () -> new IntervalTree<>(Interval::start, Interval::end, (a, b) -> b - a),
                (tree, datum) -> tree.add(tree.getInterval(datum)),
                mergingNotSupported()).getConsecutiveIntervalData();
    }

    private static <T> BiConsumer<T, T> mergingNotSupported() {
        return (a, b) -> {
            throw new UnsupportedOperationException();
        };
    }

    private static <A, Container_> Runnable accumulate(
            UniConstraintCollector<A, Container_, ?> collector, Container_ container, A value) {
        return collector.accumulator().apply(container, value);
    }

    private static <A, Container_, Result_> void assertResult(
            UniConstraintCollector<A, Container_, Result_> collector, Container_ container, Result_ expectedResult) {
        Result_ actualResult = collector.finisher().apply(container);
        assertThat(actualResult)
                .as("Collector (" + collector + ") did not produce expected result.")
                .usingRecursiveComparison()
                .ignoringFields("sourceTree", "indexFunction", "sequenceList", "startItemToSequence")
                .isEqualTo(expectedResult);
    }

    private record Interval(int start, int end) {

    }

}
