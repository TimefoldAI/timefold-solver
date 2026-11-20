package ai.timefold.solver.core.impl.score.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.SimpleScore;

import org.junit.jupiter.api.Test;

class ScoreDefinitionDivideEdgeCaseTest {

    @Test
    void largeLongQuotientKeepsFullPrecision() {
        var scoreDefinition = new SimpleScoreDefinition();
        var dividend = SimpleScore.of(9007199254740993L);
        var divisor = SimpleScore.of(3L);

        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, divisor))
                .isEqualTo(SimpleScore.of(3002399751580331L));
    }

    @Test
    void negativeLongQuotientFloorsWithoutDoubleConversion() {
        var scoreDefinition = new SimpleScoreDefinition();
        var dividend = SimpleScore.of(-9007199254740993L);
        var divisor = SimpleScore.of(3L);

        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, divisor))
                .isEqualTo(SimpleScore.of(-3002399751580331L));
    }

    @Test
    void bigDecimalQuotientDoesNotDependOnDivisorScale() {
        var scoreDefinition = new SimpleBigDecimalScoreDefinition();
        var dividend = SimpleBigDecimalScore.of(new BigDecimal("2.5"));

        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend,
                SimpleBigDecimalScore.of(new BigDecimal("2.0"))))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("1.2")));
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend,
                SimpleBigDecimalScore.of(new BigDecimal("2.000"))))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("1.2")));
    }
}
