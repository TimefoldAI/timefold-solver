package ai.timefold.solver.core.impl.bavet.common.index;

import static ai.timefold.solver.core.impl.bavet.common.index.SelectionProbabilityTest.toEntries;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Filtering a {@link UniqueRandomIterator} with a {@link FilteringIterator} (no bail-out, since the
 * delegate already ends by itself) has a whole lot of possible corner cases.
 * Therefore, we run the same tests with a variety of random seeds to increase coverage.
 * None of these are expected to fail.
 * If any fail, the random seed is printed in the test name for reproducibility.
 */
@MethodSource("randomSeeds")
@ParameterizedClass
@Execution(ExecutionMode.CONCURRENT)
class FilteredUniqueIterationTest {

    private static final List<String> ELEMENTS = List.of("A", "B", "C", "D");

    private static Stream<Arguments> randomSeeds() {
        return IntStream.range(0, 10)
                .mapToObj(Arguments::of);
    }

    @Parameter
    private int randomSeed;

    Random random;

    @BeforeEach
    void beforeEach() {
        random = new Random(randomSeed);
    }

    @CsvSource(useHeadersInDisplayName = true, value = """
            elementCount
            1
            2
            3
            4
            """)
    @ParameterizedTest(name = "{arguments}")
    void throwsWhenUnknowinglyEmpty(int elementCount) {
        var list = ELEMENTS.subList(0, elementCount);
        Predicate<String> filter = Predicate.not(list::contains);

        // Everything is filtered out, but the sequence has no way of knowing that.
        var sequence = new FilteringIterator<>(UniqueRandomIterator.of(toEntries(list), random), filter);
        assertThatThrownBy(sequence::next)
                .isInstanceOf(NoSuchElementException.class);
    }

}
