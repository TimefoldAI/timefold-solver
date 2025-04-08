package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;

import org.junit.jupiter.api.Test;

class HardMediumSoftBigDecimalScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new HardMediumSoftBigDecimalScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(HardMediumSoftBigDecimalScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new HardMediumSoftBigDecimalScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(HardMediumSoftBigDecimalScore.ONE_SOFT);
    }

    @Test
    void getLevelsSize() {
        assertThat(new HardMediumSoftBigDecimalScoreDefinition().getLevelsSize()).isEqualTo(3);
    }

    @Test
    void getLevelLabels() {
        assertThat(new HardMediumSoftBigDecimalScoreDefinition().getLevelLabels())
                .containsExactly("hard score", "medium score", "soft score");
    }

    @Test
    void getFeasibleLevelsSize() {
        assertThat(new HardMediumSoftBigDecimalScoreDefinition().getFeasibleLevelsSize()).isEqualTo(1);
    }

    // Optimistic and pessimistic bounds are currently not supported for this score definition

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new HardMediumSoftBigDecimalScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(
                new Number[] { BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(
                new Number[] { BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(
                        new Number[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE }));
    }

}
