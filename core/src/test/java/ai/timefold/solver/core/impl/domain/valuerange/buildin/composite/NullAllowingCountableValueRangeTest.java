package ai.timefold.solver.core.impl.domain.valuerange.buildin.composite;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllElementsOfIterator;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertElementsOfIterator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.collection.ListValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SelectionSorterAdapter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorFactorySelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class NullAllowingCountableValueRangeTest {

    @Test
    void getSize() {
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(0, 2, 5, 10))).getSize())
                .isEqualTo(5L);
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(100, 120, 5, 7, 8))).getSize())
                .isEqualTo(6L);
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(-15, 25, 0))).getSize())
                .isEqualTo(4L);
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList("b", "z", "a"))).getSize())
                .isEqualTo(4L);
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Collections.emptyList())).getSize())
                .isEqualTo(1L);
    }

    @Test
    void get() {
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(0, 2, 5, 10))).get(0L))
                .isNull();
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(0, 2, 5, 10))).get(2L))
                .isEqualTo(2);
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList("b", "z", "a", "c", "g", "d")))
                .get(3L))
                .isEqualTo("a");
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList("b", "z", "a", "c", "g", "d")))
                .get(6L))
                .isEqualTo("d");
    }

    @Test
    void contains() {
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(0, 2, 5, 10))).contains(5))
                .isTrue();
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(0, 2, 5, 10))).contains(4))
                .isFalse();
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(0, 2, 5, 10))).contains(null))
                .isTrue();
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList("b", "z", "a"))).contains("a"))
                .isTrue();
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList("b", "z", "a"))).contains("n"))
                .isFalse();
        assertThat(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList("b", "z", "a"))).contains(null))
                .isTrue();
    }

    @Test
    void createOriginalIterator() {
        assertAllElementsOfIterator(
                new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(0, 2, 5, 10)))
                        .createOriginalIterator(),
                null, 0, 2, 5, 10);
        assertAllElementsOfIterator(
                new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(100, 120, 5, 7, 8)))
                        .createOriginalIterator(),
                null, 100, 120, 5, 7, 8);
        assertAllElementsOfIterator(
                new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(-15, 25, 0))).createOriginalIterator(),
                null, -15, 25, 0);
        assertAllElementsOfIterator(
                new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList("b", "z", "a")))
                        .createOriginalIterator(),
                null, "b", "z", "a");
        assertAllElementsOfIterator(new NullAllowingCountableValueRange<>(new ListValueRange<>(Collections.emptyList()))
                .createOriginalIterator(), new String[] { null });
    }

    @Test
    void createRandomIterator() {
        assertElementsOfIterator(new NullAllowingCountableValueRange<>(new ListValueRange<>(Arrays.asList(0, 2, 5)))
                .createRandomIterator(new TestRandom(3, 2, 1, 0)), 5, 2, 0, null);
        assertElementsOfIterator(new NullAllowingCountableValueRange<>(new ListValueRange<>(Collections.emptyList()))
                .createRandomIterator(new TestRandom(0)), new String[] { null });
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
        assertAllElementsOfIterator(((CountableValueRange<Integer>) new NullAllowingCountableValueRange<>(
                (new ListValueRange<>(Arrays.asList(-15, 25, 0, 1, -1)))).sort(ascComparatorSorter)).createOriginalIterator(),
                -15, -1, 0, 1, 25);
        assertAllElementsOfIterator(((CountableValueRange<Integer>) new NullAllowingCountableValueRange<>(
                (new ListValueRange<>(Arrays.asList(-15, 25, 0, 1, -1)))).sort(ascComparatorFactorySorter))
                .createOriginalIterator(), -15, -1, 0, 1, 25);
        assertAllElementsOfIterator(((CountableValueRange<Integer>) new NullAllowingCountableValueRange<>(
                (new ListValueRange<>(Arrays.asList(-15, 25, 0, 1, -1)))).sort(descComparatorSorter)).createOriginalIterator(),
                25, 1, 0, -1, -15);
        assertAllElementsOfIterator(((CountableValueRange<Integer>) new NullAllowingCountableValueRange<>(
                (new ListValueRange<>(Arrays.asList(-15, 25, 0, 1, -1)))).sort(descComparatorFactorySorter))
                .createOriginalIterator(), 25, 1, 0, -1, -15);
    }

}
