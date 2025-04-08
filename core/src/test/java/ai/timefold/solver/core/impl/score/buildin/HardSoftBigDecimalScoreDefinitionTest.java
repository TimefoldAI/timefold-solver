package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;

import org.junit.jupiter.api.Test;

class HardSoftBigDecimalScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new HardSoftBigDecimalScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(HardSoftBigDecimalScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new HardSoftBigDecimalScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(HardSoftBigDecimalScore.ONE_SOFT);
    }

    @Test
    void getLevelsSize() {
        assertThat(new HardSoftBigDecimalScoreDefinition().getLevelsSize()).isEqualTo(2);
    }

    @Test
    void getLevelLabels() {
        assertThat(new HardSoftBigDecimalScoreDefinition().getLevelLabels()).containsExactly("hard score", "soft score");
    }

    @Test
    void getFeasibleLevelsSize() {
        assertThat(new HardSoftBigDecimalScoreDefinition().getFeasibleLevelsSize()).isEqualTo(1);
    }

    // Optimistic and pessimistic bounds are currently not supported for this score definition

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new HardSoftBigDecimalScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(
                new Number[] { BigDecimal.ZERO, BigDecimal.TEN });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(
                new Number[] { BigDecimal.TEN, BigDecimal.TEN });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(
                        new Number[] { BigDecimal.ZERO, BigDecimal.ONE }));
    }

}
