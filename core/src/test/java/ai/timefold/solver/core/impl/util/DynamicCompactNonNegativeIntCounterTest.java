package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DynamicCompactNonNegativeIntCounterTest {

    @Test
    void count() {
        var counter = new DynamicCompactNonNegativeIntCounter();

        // Empty counter
        assertThat(counter.getCount(0)).isZero();
        assertThat(counter.getCount(1)).isZero();
        assertThat(counter.getCount(2)).isZero();
        assertThat(counter.capacity()).isZero();

        // Create entries in counter
        counter.increment(1);

        counter.increment(2);
        counter.increment(2);

        assertThat(counter.getCount(0)).isZero();
        assertThat(counter.getCount(1)).isEqualTo(1);
        assertThat(counter.getCount(2)).isEqualTo(2);
        assertThat(counter.capacity()).isEqualTo(2);

        counter.decrement(2);
        counter.decrement(1);

        // 1 now has count 0, so its entry can be reclaimed
        assertThat(counter.getCount(0)).isZero();
        assertThat(counter.getCount(1)).isZero();
        assertThat(counter.getCount(2)).isEqualTo(1);
        assertThat(counter.capacity()).isEqualTo(2);

        // assert 0 uses the reclaim 1 entry instead of creating a new one
        counter.increment(0);

        assertThat(counter.getCount(0)).isEqualTo(1);
        assertThat(counter.getCount(1)).isZero();
        assertThat(counter.getCount(2)).isEqualTo(1);
        assertThat(counter.capacity()).isEqualTo(2);
    }

    @Test
    void testToString() {
        var counter = new DynamicCompactNonNegativeIntCounter();

        assertThat(counter).hasToString("{}");

        counter.increment(1);

        counter.increment(2);
        counter.increment(2);

        assertThat(counter).hasToString("{1: 1, 2: 2}");

        counter.decrement(2);
        counter.decrement(1);

        assertThat(counter).hasToString("{2: 1}");

        counter.increment(0);

        assertThat(counter).hasToString("{0: 1, 2: 1}");
    }
}