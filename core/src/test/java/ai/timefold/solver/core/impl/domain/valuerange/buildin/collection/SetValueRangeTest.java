package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllElementsOfIterator;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertElementsOfIterator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class SetValueRangeTest {

    @Test
    void getSize() {
        assertThat(new SetValueRange<>(of(0, 2, 5, 10)).getSize()).isEqualTo(4L);
        assertThat(new SetValueRange<>(of(100, 120, 5, 7, 8)).getSize()).isEqualTo(5L);
        assertThat(new SetValueRange<>(of(-15, 25, 0)).getSize()).isEqualTo(3L);
        assertThat(new SetValueRange<>(of("b", "z", "a")).getSize()).isEqualTo(3L);
        assertThat(new SetValueRange<>(Collections.emptySet()).getSize()).isEqualTo(0L);
    }

    @Test
    void get() {
        assertThat(new SetValueRange<>(of(0, 2, 5, 10)).get(2L).intValue()).isEqualTo(5);
        assertThat(new SetValueRange<>(of(100, -120)).get(1L).intValue()).isEqualTo(-120);
        assertThat(new SetValueRange<>(of("b", "z", "a", "c", "g", "d")).get(3L)).isEqualTo("c");
    }

    @Test
    void contains() {
        assertThat(new SetValueRange<>(of(0, 2, 5, 10)).contains(5)).isTrue();
        assertThat(new SetValueRange<>(of(0, 2, 5, 10)).contains(4)).isFalse();
        assertThat(new SetValueRange<>(of(100, 120, 5, 7, 8)).contains(7)).isTrue();
        assertThat(new SetValueRange<>(of(100, 120, 5, 7, 8)).contains(9)).isFalse();
        assertThat(new SetValueRange<>(of(-15, 25, 0)).contains(-15)).isTrue();
        assertThat(new SetValueRange<>(of(-15, 25, 0)).contains(-14)).isFalse();
        assertThat(new SetValueRange<>(of("b", "z", "a")).contains("a")).isTrue();
        assertThat(new SetValueRange<>(of("b", "z", "a")).contains("n")).isFalse();
    }

    @Test
    void createOriginalIterator() {
        assertAllElementsOfIterator(new SetValueRange<>(of(0, 2, 5, 10)).createOriginalIterator(), 0, 2, 5, 10);
        assertAllElementsOfIterator(new SetValueRange<>(of(100, 120, 5, 7, 8)).createOriginalIterator(), 100, 120, 5, 7, 8);
        assertAllElementsOfIterator(new SetValueRange<>(of(-15, 25, 0)).createOriginalIterator(), -15, 25, 0);
        assertAllElementsOfIterator(new SetValueRange<>(of("b", "z", "a")).createOriginalIterator(), "b", "z", "a");
        assertAllElementsOfIterator(new SetValueRange<>(Collections.emptySet()).createOriginalIterator());
    }

    @Test
    void createRandomIterator() {
        assertElementsOfIterator(new SetValueRange<>(of(0, 2, 5, 10)).createRandomIterator(new TestRandom(2, 0)), 5, 0);
        assertElementsOfIterator(new SetValueRange<>(of(100, 120, 5, 7, 8)).createRandomIterator(new TestRandom(2, 0)), 5, 100);
        assertElementsOfIterator(new SetValueRange<>(of(-15, 25, 0)).createRandomIterator(new TestRandom(2, 0)), 0, -15);
        assertElementsOfIterator(new SetValueRange<>(of("b", "z", "a")).createRandomIterator(new TestRandom(2, 0)), "a", "b");
        assertAllElementsOfIterator(new SetValueRange<>(Collections.emptySet()).createRandomIterator(new Random(0)));
    }

    @SafeVarargs
    private static <T> Set<T> of(T... elements) {
        var set = new LinkedHashSet<T>(elements.length);
        set.addAll(Arrays.asList(elements));
        return set;
    }

}
