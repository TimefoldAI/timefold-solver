package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
 * The class under test uses randomness, and has a whole lot of possible corner cases.
 * Therefore, we run the same tests with a variety of random seeds to increase coverage.
 * None of these are expected to fail.
 * If any fail, the random seed is printed in the test name for reproducibility.
 */
@MethodSource("randomSeeds")
@ParameterizedClass
@Execution(ExecutionMode.CONCURRENT)
class FilteredUniqueRandomSequenceTest {

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
            totalElementCount,      filteredElementCount
            2,                      1
            3,                      1
            3,                      2
            4,                      1
            4,                      2
            4,                      3
            """)
    @ParameterizedTest(name = "{arguments}")
    void exhaust(int totalElementCount, int filteredElementCount) {
        var list = ELEMENTS.subList(0, totalElementCount);
        var filteredElements = new HashSet<String>();
        while (filteredElements.size() < filteredElementCount) {
            filteredElements.add(list.get(random.nextInt(list.size())));
        }
        Predicate<String> filter = Predicate.not(filteredElements::contains);

        var sequence = new FilteredUniqueRandomSequence<>(list, filter);
        var expectedUnfilteredSize = list.size() - filteredElements.size();

        var picked = new LinkedHashSet<>(expectedUnfilteredSize);
        for (int i = 0; i < expectedUnfilteredSize; i++) {
            var element = sequence.pick(random);
            picked.add(element.value());
            sequence.remove(element.index());
        }

        assertSoftly(softly -> {
            softly.assertThat(picked).hasSize(expectedUnfilteredSize);
            softly.assertThat(picked).doesNotContainAnyElementsOf(filteredElements);
        });
    }

    @CsvSource(useHeadersInDisplayName = true, value = """
            elementCount
            1
            2
            3
            4
            """)
    @ParameterizedTest(name = "{arguments}")
    void throwsWhenExhausted(int elementCount) {
        var list = ELEMENTS.subList(0, elementCount);
        var filteredElements = new HashSet<String>();
        while (filteredElements.size() < elementCount) {
            filteredElements.add(list.get(random.nextInt(list.size())));
        }
        Predicate<String> filter = Predicate.not(filteredElements::contains);

        var sequence = new FilteredUniqueRandomSequence<>(list, filter);
        var expectedUnfilteredSize = list.size() - filteredElements.size();

        for (int i = 0; i < expectedUnfilteredSize; i++) {
            sequence.remove(i);
        }

        // Once all the unfiltered elements are removed, picking should throw.
        assertThatThrownBy(() -> sequence.pick(random))
                .isInstanceOf(NoSuchElementException.class);
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
        var sequence = new FilteredUniqueRandomSequence<>(list, filter);
        assertThatThrownBy(() -> sequence.pick(random))
                .isInstanceOf(NoSuchElementException.class);
    }

}
