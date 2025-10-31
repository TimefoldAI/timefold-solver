package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllElementsOfIterator;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertElementsOfIterator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SelectionSorterAdapter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorFactorySelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class SetValueRangeTest {

    @Test
    void getSize() {
        assertThat(createRange(0, 2, 5, 10).getSize()).isEqualTo(4L);
        assertThat(createRange(100, 120, 5, 7, 8).getSize()).isEqualTo(5L);
        assertThat(createRange(-15, 25, 0).getSize()).isEqualTo(3L);
        assertThat(createRange("b", "z", "a").getSize()).isEqualTo(3L);
        assertThat(new SetValueRange<>(Collections.emptySet()).getSize()).isZero();
    }

    @Test
    void get() {
        assertThat(createRange(0, 2, 5, 10).get(2L).intValue()).isEqualTo(5);
        assertThat(createRange(100, -120).get(1L).intValue()).isEqualTo(-120);
        assertThat(createRange("b", "z", "a", "c", "g", "d").get(3L)).isEqualTo("c");
    }

    @Test
    void contains() {
        assertThat(createRange(0, 2, 5, 10).contains(5)).isTrue();
        assertThat(createRange(0, 2, 5, 10).contains(4)).isFalse();
        assertThat(createRange(0, 2, 5, 10).contains(null)).isFalse();
        assertThat(createRange(100, 120, 5, 7, 8).contains(7)).isTrue();
        assertThat(createRange(100, 120, 5, 7, 8).contains(9)).isFalse();
        assertThat(createRange(-15, 25, 0).contains(-15)).isTrue();
        assertThat(createRange(-15, 25, 0).contains(-14)).isFalse();
        assertThat(createRange("b", "z", "a").contains("a")).isTrue();
        assertThat(createRange("b", "z", "a").contains("n")).isFalse();
    }

    @Test
    void createOriginalIterator() {
        assertAllElementsOfIterator(createRange(0, 2, 5, 10).createOriginalIterator(), 0, 2, 5, 10);
        assertAllElementsOfIterator(createRange(100, 120, 5, 7, 8).createOriginalIterator(), 100, 120,
                5, 7, 8);
        assertAllElementsOfIterator(createRange(-15, 25, 0).createOriginalIterator(), -15, 25, 0);
        assertAllElementsOfIterator(createRange("b", "z", "a").createOriginalIterator(), "b", "z", "a");
        assertAllElementsOfIterator(new SetValueRange<>(Collections.emptySet()).createOriginalIterator());
    }

    @Test
    void createRandomIterator() {
        assertElementsOfIterator(createRange(0, 2, 5, 10).createRandomIterator(new TestRandom(2, 0)), 5,
                0);
        assertElementsOfIterator(
                createRange(100, 120, 5, 7, 8).createRandomIterator(new TestRandom(2, 0)), 5,
                100);
        assertElementsOfIterator(createRange(-15, 25, 0).createRandomIterator(new TestRandom(2, 0)), 0,
                -15);
        assertElementsOfIterator(createRange("b", "z", "a").createRandomIterator(new TestRandom(2, 0)),
                "a", "b");
        assertAllElementsOfIterator(new SetValueRange<>(Collections.emptySet()).createRandomIterator(new Random(0)));
    }

    @Test
    void sort() {
        var ascComparatorSorter = new SelectionSorterAdapter<>(null, new ComparatorSelectionSorter<>(
                Comparator.comparingInt(Integer::intValue), SelectionSorterOrder.ASCENDING));
        Comparator<Integer> integerComparator = Integer::compareTo;
        var ascComparatorFactorySorter = new SelectionSorterAdapter<>(null,
                new ComparatorFactorySelectionSorter<>(solution -> integerComparator, SelectionSorterOrder.ASCENDING));
        var descComparatorSorter = new SelectionSorterAdapter<>(null, new ComparatorSelectionSorter<>(
                Comparator.comparingInt(Integer::intValue), SelectionSorterOrder.DESCENDING));
        var descComparatorFactorySorter = new SelectionSorterAdapter<>(null,
                new ComparatorFactorySelectionSorter<>(solution -> integerComparator, SelectionSorterOrder.DESCENDING));
        assertAllElementsOfIterator(((CountableValueRange<Integer>) createRange(-15, 25, 0, 1, -1)
                .sort(ascComparatorSorter)).createOriginalIterator(), -15, -1, 0, 1, 25);
        assertAllElementsOfIterator(((CountableValueRange<Integer>) createRange(-15, 25, 0, 1, -1)
                .sort(ascComparatorFactorySorter)).createOriginalIterator(), -15, -1, 0, 1, 25);
        assertAllElementsOfIterator(((CountableValueRange<Integer>) createRange(-15, 25, 0, 1, -1)
                .sort(descComparatorSorter)).createOriginalIterator(), 25, 1, 0, -1, -15);
        assertAllElementsOfIterator(((CountableValueRange<Integer>) createRange(-15, 25, 0, 1, -1)
                .sort(descComparatorFactorySorter)).createOriginalIterator(), 25, 1, 0, -1, -15);
    }

    private static <T> SetValueRange<T> createRange(T... values) {
        var set = Arrays.stream(values)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new SetValueRange<>(set);
    }

}
