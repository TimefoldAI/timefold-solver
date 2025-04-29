package ai.timefold.solver.core.api.score.buildin.simplebigdecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.AbstractScoreTest;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.junit.jupiter.api.Test;

class SimpleBigDecimalScoreTest extends AbstractScoreTest {

    @Test
    void parseScore() {
        assertThat(SimpleBigDecimalScore.parseScore("-147.2")).isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("-147.2")));
    }

    @Test
    void toShortString() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("0.0")).toShortString()).isEqualTo("0");
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("-147.2")).toShortString()).isEqualTo("-147.2");
    }

    @Test
    void testToString() {
        assertSoftly(softly -> {
            softly.assertThat(SimpleBigDecimalScore.of(BigDecimal.ZERO)).hasToString("0");
            softly.assertThat(SimpleBigDecimalScore.of(new BigDecimal("0.0"))).hasToString("0");
            softly.assertThat(SimpleBigDecimalScore.of(new BigDecimal("0.00"))).hasToString("0");
            softly.assertThat(SimpleBigDecimalScore.of(new BigDecimal("-147.2"))).hasToString("-147.2");
        });
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> SimpleBigDecimalScore.parseScore("-147.2hard/-258.3soft"));
    }

    @Test
    void add() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("20")).add(
                SimpleBigDecimalScore.of(new BigDecimal("-1")))).isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("19")));
    }

    @Test
    void subtract() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("20")).subtract(
                SimpleBigDecimalScore.of(new BigDecimal("-1")))).isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("21")));
    }

    @Test
    void multiply() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("5.0")).multiply(1.2))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("6.0")));
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("1.0")).multiply(1.2))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("1.2")));
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("4.0")).multiply(1.2))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("4.8")));
    }

    @Test
    void divide() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("25.0")).divide(5.0))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("5.0")));
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("21.0")).divide(5.0))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("4.2")));
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("24.0")).divide(5.0))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("4.8")));
    }

    @Test
    void power() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("5.0")).power(2.0))
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("25.0")));
    }

    @Test
    void negate() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("5.0")).negate())
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("-5.0")));
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("-5.0")).negate())
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("5.0")));
    }

    @Test
    void abs() {
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("5.0")).abs())
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("5.0")));
        assertThat(SimpleBigDecimalScore.of(new BigDecimal("-5.0")).abs())
                .isEqualTo(SimpleBigDecimalScore.of(new BigDecimal("5.0")));
    }

    @Test
    void zero() {
        SimpleBigDecimalScore manualZero = SimpleBigDecimalScore.of(BigDecimal.ZERO);
        assertSoftly(softly -> {
            softly.assertThat(manualZero.zero()).isEqualTo(manualZero);
            softly.assertThat(manualZero.isZero()).isTrue();
            SimpleBigDecimalScore manualOne = SimpleBigDecimalScore.of(BigDecimal.ONE);
            softly.assertThat(manualOne.isZero()).isFalse();
        });
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(
                SimpleBigDecimalScore.of(new BigDecimal("-10.0")),
                SimpleBigDecimalScore.of(new BigDecimal("-10.0")),
                SimpleBigDecimalScore.of(new BigDecimal("-10.000")));
        PlannerAssert.assertObjectsAreNotEqual(
                SimpleBigDecimalScore.of(new BigDecimal("-10.0")),
                SimpleBigDecimalScore.of(new BigDecimal("-30.0")));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                SimpleBigDecimalScore.of(new BigDecimal("-300.5")),
                SimpleBigDecimalScore.of(new BigDecimal("-300")),
                SimpleBigDecimalScore.of(new BigDecimal("-20.067")),
                SimpleBigDecimalScore.of(new BigDecimal("-20.007")),
                SimpleBigDecimalScore.of(new BigDecimal("-20")),
                SimpleBigDecimalScore.of(new BigDecimal("-1")),
                SimpleBigDecimalScore.of(new BigDecimal("0")),
                SimpleBigDecimalScore.of(new BigDecimal("1")));
    }
}
