package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BailOutUtilsTest {

    @Test
    void sizeTimesMultiplier() {
        assertThat(BailOutUtils.bailOutSize(0)).isZero();
        assertThat(BailOutUtils.bailOutSize(3)).isEqualTo(30);
        assertThat(BailOutUtils.bailOutSize(BailOutUtils.MAX_BAIL_OUT / 10)).isEqualTo(BailOutUtils.MAX_BAIL_OUT);
        assertThat(BailOutUtils.bailOutSize(() -> 3, "subject")).isEqualTo(30);
    }

    @Test
    void hugeSizeIsCapped() {
        assertThat(BailOutUtils.bailOutSize(BailOutUtils.MAX_BAIL_OUT / 10 + 1)).isEqualTo(BailOutUtils.MAX_BAIL_OUT);
        assertThat(BailOutUtils.bailOutSize(Long.MAX_VALUE / 10 + 1)).isEqualTo(BailOutUtils.MAX_BAIL_OUT);
        assertThat(BailOutUtils.bailOutSize(() -> Long.MAX_VALUE, "subject")).isEqualTo(BailOutUtils.MAX_BAIL_OUT);
    }

    @Test
    void negativeSize() {
        assertThat(BailOutUtils.bailOutSize(-1)).isEqualTo(BailOutUtils.UNKNOWN_SIZE_BAIL_OUT);
        assertThat(BailOutUtils.bailOutSize(() -> -1, "subject")).isEqualTo(BailOutUtils.UNKNOWN_SIZE_BAIL_OUT);
    }

    @Test
    void unknownSize() {
        assertThat(BailOutUtils.bailOutSize(() -> {
            throw new UnsupportedOperationException();
        }, "subject")).isEqualTo(BailOutUtils.UNKNOWN_SIZE_BAIL_OUT);
    }

    @Test
    void failingSize() {
        // A custom MoveIteratorFactory with a negative size makes the bridge throw this.
        assertThat(BailOutUtils.bailOutSize(() -> {
            throw new IllegalStateException();
        }, "subject")).isEqualTo(BailOutUtils.UNKNOWN_SIZE_BAIL_OUT);
    }

}
