package ai.timefold.solver.core.impl.score;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ScoreUtilTest {

    @Test
    void isZero() {
        assertThat(ScoreUtil.isZero(BigDecimal.ZERO)).isTrue();
        assertThat(ScoreUtil.isZero(new BigDecimal("0.00"))).isTrue();
        assertThat(ScoreUtil.isZero(new BigDecimal("-0.0"))).isTrue();
        assertThat(ScoreUtil.isZero(BigDecimal.ONE)).isFalse();
        assertThat(ScoreUtil.isZero(BigDecimal.ONE.negate())).isFalse();
    }

    @Test
    void isNegative() {
        assertThat(ScoreUtil.isNegative(BigDecimal.ONE.negate())).isTrue();
        assertThat(ScoreUtil.isNegative(BigDecimal.ZERO)).isFalse();
        assertThat(ScoreUtil.isNegative(new BigDecimal("-0.0"))).isFalse();
        assertThat(ScoreUtil.isNegative(BigDecimal.ONE)).isFalse();
    }

    @Test
    void equalsIgnoringScale() {
        assertThat(ScoreUtil.equalsIgnoringScale(new BigDecimal("1.0"), new BigDecimal("1.00"))).isTrue();
        assertThat(ScoreUtil.equalsIgnoringScale(new BigDecimal("1.0"), new BigDecimal("1.1"))).isFalse();
    }

    @Test
    void hashCodeIgnoringScale() {
        assertThat(ScoreUtil.hashCodeIgnoringScale(new BigDecimal("1.0")))
                .isEqualTo(ScoreUtil.hashCodeIgnoringScale(new BigDecimal("1.00")));
    }

    @Test
    void nonNegativeScale() {
        assertThat(ScoreUtil.nonNegativeScale(2)).isEqualTo(2);
        assertThat(ScoreUtil.nonNegativeScale(0)).isEqualTo(0);
        assertThat(ScoreUtil.nonNegativeScale(-2)).isEqualTo(0);
    }

    @Test
    void multiplyNormalScale() {
        assertThat(ScoreUtil.multiply(new BigDecimal("5.0"), 1.5)).isEqualTo(new BigDecimal("7.5"));
    }

    @Test
    void multiplyClampsNegativeScale() {
        // Flooring onto the receiver's own scale (-2, nearest hundred) would wrongly give 100.
        assertThat(ScoreUtil.multiply(new BigDecimal("1E+2"), 1.5)).isEqualTo(new BigDecimal("150"));
    }

    @Test
    void divideNormalScale() {
        assertThat(ScoreUtil.divide(new BigDecimal("5.0"), 2.0)).isEqualTo(new BigDecimal("2.5"));
    }

    @Test
    void divideClampsNegativeScale() {
        // Flooring onto the receiver's own scale (-2, nearest hundred) would wrongly give 0.
        assertThat(ScoreUtil.divide(new BigDecimal("1E+2"), 2.0)).isEqualTo(new BigDecimal("50"));
    }

    @Test
    void powerNormalScale() {
        assertThat(ScoreUtil.power(new BigDecimal("5.0"), 2.0)).isEqualTo(new BigDecimal("25.0"));
    }

    @Test
    void powerClampsNegativeScale() {
        // Any exponent other than 0 stays an exact multiple of the receiver's own coarse scale,
        // so exponent 0 is the only one that actually reproduces the bug: 100^0 = 1, and flooring
        // 1 onto scale -2 (nearest hundred) would wrongly give 0.
        assertThat(ScoreUtil.power(new BigDecimal("1E+2"), 0.0)).isEqualTo(BigDecimal.ONE);
    }

}
