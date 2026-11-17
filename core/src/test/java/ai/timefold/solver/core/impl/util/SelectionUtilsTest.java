package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class SelectionUtilsTest {

    @Test
    void sum() {
        assertThat(SelectionUtils.sum(2, 3)).isEqualTo(5);
        assertThat(SelectionUtils.sum(Long.MAX_VALUE, 1)).isEqualTo(Long.MAX_VALUE);
        assertThat(SelectionUtils.sum(Long.MIN_VALUE, -1)).isEqualTo(Long.MIN_VALUE);
        assertThat(SelectionUtils.sum(Long.MAX_VALUE, Long.MIN_VALUE)).isEqualTo(-1);
    }

    @Test
    void sumOfItems() {
        assertThat(SelectionUtils.sumOf(List.of(1L, 2L, 3L), size -> size)).isEqualTo(6);
        var calls = new AtomicInteger();
        assertThat(SelectionUtils.sumOf(List.of(Long.MAX_VALUE, 1L, 2L), size -> {
            calls.incrementAndGet();
            return size;
        })).isEqualTo(Long.MAX_VALUE);
        assertThat(calls).hasValue(1);
    }

    @Test
    void sumOfRange() {
        assertThat(SelectionUtils.sumOf(1, 3, i -> i)).isEqualTo(6);
        assertThat(SelectionUtils.sumOf(3, 1, i -> i)).isZero();
        // Ends although the int counter of a naive loop would overflow.
        assertThat(SelectionUtils.sumOf(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, i -> 1)).isEqualTo(2);
        assertThat(SelectionUtils.sumOf(1, Integer.MAX_VALUE, i -> Long.MAX_VALUE / 2)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void product() {
        assertThat(SelectionUtils.product(6, 7)).isEqualTo(42);
        assertThat(SelectionUtils.product(-6, 7)).isEqualTo(-42);
        assertThat(SelectionUtils.product(Long.MAX_VALUE, 2)).isEqualTo(Long.MAX_VALUE);
        assertThat(SelectionUtils.product(Long.MAX_VALUE, -2)).isEqualTo(Long.MIN_VALUE);
        assertThat(SelectionUtils.product(Long.MIN_VALUE, -1)).isEqualTo(Long.MAX_VALUE);
        assertThat(SelectionUtils.product(Long.MIN_VALUE, 1)).isEqualTo(Long.MIN_VALUE);
        assertThat(SelectionUtils.product(1L << 32, 1L << 31)).isEqualTo(Long.MAX_VALUE);
        assertThat(SelectionUtils.product(2, 3, 7)).isEqualTo(42);
        assertThat(SelectionUtils.product(1L << 32, 1L << 31, 0)).isZero();
    }

    @Test
    void orderedPicks() {
        assertThat(SelectionUtils.orderedPicks(5, 3)).isEqualTo(60);
        assertThat(SelectionUtils.orderedPicks(5, 0)).isEqualTo(1);
        assertThat(SelectionUtils.orderedPicks(20, 20)).isEqualTo(2_432_902_008_176_640_000L);
        assertThat(SelectionUtils.orderedPicks(21, 21)).isEqualTo(Long.MAX_VALUE);
        assertThat(SelectionUtils.orderedPicks(1_000_000_000_000L, 999_999_999_999L)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void sizeTimesMultiplier() {
        assertThat(SelectionUtils.bailOutSize(0)).isZero();
        assertThat(SelectionUtils.bailOutSize(3)).isEqualTo(30);
        assertThat(SelectionUtils.bailOutSize(SelectionUtils.MAX_BAIL_OUT / 10)).isEqualTo(SelectionUtils.MAX_BAIL_OUT);
        assertThat(SelectionUtils.bailOutSize(() -> 3, "subject")).isEqualTo(30);
    }

    @Test
    void hugeSizeIsCapped() {
        assertThat(SelectionUtils.bailOutSize(SelectionUtils.MAX_BAIL_OUT / 10 + 1)).isEqualTo(SelectionUtils.MAX_BAIL_OUT);
        assertThat(SelectionUtils.bailOutSize(Long.MAX_VALUE / 10 + 1)).isEqualTo(SelectionUtils.MAX_BAIL_OUT);
        assertThat(SelectionUtils.bailOutSize(() -> Long.MAX_VALUE, "subject")).isEqualTo(SelectionUtils.MAX_BAIL_OUT);
    }

    @Test
    void negativeSize() {
        assertThat(SelectionUtils.bailOutSize(-1)).isEqualTo(SelectionUtils.UNKNOWN_SIZE_BAIL_OUT);
        assertThat(SelectionUtils.bailOutSize(() -> -1, "subject")).isEqualTo(SelectionUtils.UNKNOWN_SIZE_BAIL_OUT);
    }

    @Test
    void unknownSize() {
        assertThat(SelectionUtils.bailOutSize(() -> {
            throw new UnsupportedOperationException();
        }, "subject")).isEqualTo(SelectionUtils.UNKNOWN_SIZE_BAIL_OUT);
    }

    @Test
    void failingSize() {
        // A custom MoveIteratorFactory with a negative size makes the bridge throw this.
        assertThat(SelectionUtils.bailOutSize(() -> {
            throw new IllegalStateException();
        }, "subject")).isEqualTo(SelectionUtils.UNKNOWN_SIZE_BAIL_OUT);
    }

}
