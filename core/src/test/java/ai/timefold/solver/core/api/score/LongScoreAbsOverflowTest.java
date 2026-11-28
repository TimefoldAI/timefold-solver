package ai.timefold.solver.core.api.score;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LongScoreAbsOverflowTest {

    @Test
    void simpleScore() {
        assertThatThrownBy(() -> SimpleScore.of(Long.MIN_VALUE).abs())
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Long.MIN_VALUE");
    }

    @Test
    void hardSoftScore() {
        assertThatThrownBy(() -> HardSoftScore.of(Long.MIN_VALUE, 1L).abs())
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Long.MIN_VALUE");
    }

    @Test
    void hardMediumSoftScore() {
        assertThatThrownBy(() -> HardMediumSoftScore.of(1L, Long.MIN_VALUE, 1L).abs())
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Long.MIN_VALUE");
    }

    @Test
    void bendableScore() {
        var score = new BendableScore(new long[] { 1L }, new long[] { Long.MIN_VALUE });
        assertThatThrownBy(score::abs)
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Long.MIN_VALUE");
    }
}
