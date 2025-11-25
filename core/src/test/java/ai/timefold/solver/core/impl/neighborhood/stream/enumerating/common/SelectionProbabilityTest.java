package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Execution(ExecutionMode.CONCURRENT)
public final class SelectionProbabilityTest {

    private static final int TRIAL_COUNT = 1_000_000;
    private static final int SAMPLE_COUNT = 100;
    private static final List<Integer> SAMPLES = IntStream.range(0, SAMPLE_COUNT)
            .boxed()
            .toList();

    private static IntStream selectionCount() {
        var oneStream = IntStream.of(1);
        var multipleStream = IntStream.iterate(10, i -> i <= SAMPLE_COUNT, i -> i + 10);
        return IntStream.concat(oneStream, multipleStream);
    }

    @MethodSource("selectionCount") // Determines how many random picks are made.
    @ParameterizedTest
    void check(int n) {
        SortedMap<Integer, Integer> counts = new TreeMap<>();

        var sampleList = toEntries(SAMPLES);
        var random = new Random(0);
        for (var trial = 0; trial < TRIAL_COUNT; trial++) { // Independent trials; each gets its own random seed.
            UniqueRandomSequence.SequenceElement<Integer> element = null;
            var sequence = new DefaultUniqueRandomSequence<>(sampleList); // This is the code that we test.
            var splitRandom = new Random(random.nextLong());
            for (var i = 0; i < n; i++) { // Pick N random elements.
                element = sequence.pick(splitRandom);
            }
            // Record the last picked element (nth element).
            counts.compute(element.value(), (k, v) -> v == null ? 1 : v + 1);
        }

        // Guarantee that all numbers have been selected.
        Assertions.assertThat(counts).hasSize(SAMPLE_COUNT);

        // Guarantee that the distribution is roughly uniform.
        var statistics = new SummaryStatistics();
        for (var value : counts.values()) {
            statistics.addValue(value);
        }

        var context = MathContext.DECIMAL32;
        var standardDeviation = new BigDecimal(statistics.getStandardDeviation())
                .round(context);
        var threshold = BigDecimal.valueOf(TRIAL_COUNT / (double) SAMPLE_COUNT)
                .multiply(BigDecimal.valueOf(0.02))
                .round(context); // 2% tolerance
        Assertions.assertThat(standardDeviation)
                .as(() -> "Standard deviation of selection counts (%s) on the %sth random sample is over %s threshold."
                        .formatted(standardDeviation, n, threshold))
                .isLessThanOrEqualTo(threshold);
    }

    static <T> List<ElementAwareArrayList.Entry<T>> toEntries(List<T> elements) {
        var list = new ElementAwareArrayList<T>();
        for (var element : elements) {
            list.add(element);
        }
        return list.asList();
    }

}
