package ai.timefold.solver.core.impl.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ScoreUtilPowerTest {

    @Test
    void fractionalExponent() {
        assertThat(ScoreUtil.power(new BigDecimal("4.00"), 0.5)).isEqualTo(new BigDecimal("2.00"));
        assertThat(ScoreUtil.power(new BigDecimal("9.0"), 0.5)).isEqualTo(new BigDecimal("3.0"));
    }

    @Test
    void negativeExponent() {
        assertThat(ScoreUtil.power(new BigDecimal("4.0"), -1.0)).isEqualTo(new BigDecimal("0.2"));
        assertThat(ScoreUtil.power(new BigDecimal("2.0"), -2.0)).isEqualTo(new BigDecimal("0.2"));
    }

    @Test
    void negativeBaseWithIntegerExponent() {
        assertThat(ScoreUtil.power(new BigDecimal("-4.0"), 2.0)).isEqualTo(new BigDecimal("16.0"));
    }

    @Test
    void largeBaseWithFractionalExponent() {
        assertThat(ScoreUtil.power(new BigDecimal("1E+100"), 0.5))
                .isEqualByComparingTo(new BigDecimal("1E+50"));
    }

    @Test
    void negativeBaseWithFractionalExponentFailsFast() {
        assertThatThrownBy(() -> ScoreUtil.power(new BigDecimal("-4.0"), 0.5))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("non-integer exponent");
    }

    @Test
    void zeroBaseWithNegativeExponentFailsFast() {
        assertThatThrownBy(() -> ScoreUtil.power(BigDecimal.ZERO, -1.0))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("negative exponent");
    }

    @Test
    void nonFiniteExponentFailsFast() {
        assertThatThrownBy(() -> ScoreUtil.power(BigDecimal.TEN, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> ScoreUtil.power(BigDecimal.TEN, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }
}
