package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;

import org.junit.jupiter.api.Test;

class SimpleBigDecimalScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new SimpleBigDecimalScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(SimpleBigDecimalScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new SimpleBigDecimalScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(SimpleBigDecimalScore.ONE);
    }

    @Test
    void getLevelsSize() {
        assertThat(new SimpleBigDecimalScoreDefinition().getLevelsSize()).isEqualTo(1);
    }

    @Test
    void getLevelLabels() {
        assertThat(new SimpleBigDecimalScoreDefinition().getLevelLabels()).containsExactly("score");
    }

    // Optimistic and pessimistic bounds are currently not supported for this score definition

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new SimpleBigDecimalScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(new Number[] { BigDecimal.TEN });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(new Number[] { BigDecimal.TEN });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(new Number[] { BigDecimal.ONE }));
    }

}
