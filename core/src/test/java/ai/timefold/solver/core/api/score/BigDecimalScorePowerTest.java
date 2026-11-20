package ai.timefold.solver.core.api.score;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BigDecimalScorePowerTest {

    @Test
    void simpleScoreFractionalExponent() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("4.00")).power(0.5))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("2.00")));
    }

    @Test
    void hardSoftScoreFractionalExponent() {
        assertThat(HardSoftBigDecimalScore.of(new BigDecimal("4.00"), new BigDecimal("9.00")).power(0.5))
                .isEqualTo(HardSoftBigDecimalScore.of(new BigDecimal("2.00"), new BigDecimal("3.00")));
    }

    @Test
    void hardMediumSoftScoreFractionalExponent() {
        assertThat(HardMediumSoftBigDecimalScore.of(new BigDecimal("4.00"), new BigDecimal("9.00"), new BigDecimal("16.00"))
                .power(0.5))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("2.00"), new BigDecimal("3.00"),
                        new BigDecimal("4.00")));
    }

    @Test
    void bendableScoreFractionalExponent() {
        var score = new BendableBigDecimalScore(new BigDecimal[] { new BigDecimal("4.00") },
                new BigDecimal[] { new BigDecimal("9.00") });
        assertThat(score.power(0.5))
                .isEqualTo(new BendableBigDecimalScore(new BigDecimal[] { new BigDecimal("2.00") },
                        new BigDecimal[] { new BigDecimal("3.00") }));
    }

    @Test
    void simpleScoreNegativeExponent() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("4.0")).power(-1.0))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("0.2")));
    }
}
