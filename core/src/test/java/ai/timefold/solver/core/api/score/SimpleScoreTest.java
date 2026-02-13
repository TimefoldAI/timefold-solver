package ai.timefold.solver.core.api.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.testutil.PlannerAssert;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class SimpleScoreTest extends AbstractScoreTest {

    @Test
    void parseScore() {
        assertThat(SimpleScore.parseScore("-147")).isEqualTo(SimpleScore.of(-147L));
        assertThat(SimpleScore.parseScore("*")).isEqualTo(SimpleScore.of(Long.MIN_VALUE));
    }

    @Test
    void toShortString() {
        assertThat(SimpleScore.of(0L).toShortString()).isEqualTo("0");
        assertThat(SimpleScore.of(-147L).toShortString()).isEqualTo("-147");
    }

    @Test
    void testToString() {
        assertThat(SimpleScore.of(0)).hasToString("0");
        assertThat(SimpleScore.of(-147L)).hasToString("-147");
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> SimpleScore.parseScore("-147hard/-258soft"));
    }

    @Test
    void add() {
        assertThat(SimpleScore.of(20L).add(
                SimpleScore.of(-1L))).isEqualTo(SimpleScore.of(19L));
    }

    @Test
    void subtract() {
        assertThat(SimpleScore.of(20L).subtract(
                SimpleScore.of(-1L))).isEqualTo(SimpleScore.of(21L));
    }

    @Test
    void multiply() {
        assertThat(SimpleScore.of(5L).multiply(1.2)).isEqualTo(SimpleScore.of(6L));
        assertThat(SimpleScore.of(1L).multiply(1.2)).isEqualTo(SimpleScore.of(1L));
        assertThat(SimpleScore.of(4L).multiply(1.2)).isEqualTo(SimpleScore.of(4L));
    }

    @Test
    void divide() {
        assertThat(SimpleScore.of(25L).divide(5.0)).isEqualTo(SimpleScore.of(5L));
        assertThat(SimpleScore.of(21L).divide(5.0)).isEqualTo(SimpleScore.of(4L));
        assertThat(SimpleScore.of(24L).divide(5.0)).isEqualTo(SimpleScore.of(4L));
    }

    @Test
    void power() {
        assertThat(SimpleScore.of(5L).power(2.0)).isEqualTo(SimpleScore.of(25L));
        assertThat(SimpleScore.of(25L).power(0.5)).isEqualTo(SimpleScore.of(5L));
    }

    @Test
    void negate() {
        assertThat(SimpleScore.of(5L).negate()).isEqualTo(SimpleScore.of(-5L));
        assertThat(SimpleScore.of(-5L).negate()).isEqualTo(SimpleScore.of(5L));
    }

    @Test
    void abs() {
        assertThat(SimpleScore.of(5L).abs()).isEqualTo(SimpleScore.of(5L));
        assertThat(SimpleScore.of(-5L).abs()).isEqualTo(SimpleScore.of(5L));
    }

    @Test
    void zero() {
        SimpleScore manualZero = SimpleScore.of(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(manualZero.zero()).isEqualTo(manualZero);
            softly.assertThat(manualZero.isZero()).isTrue();
            SimpleScore manualOne = SimpleScore.of(1);
            softly.assertThat(manualOne.isZero()).isFalse();
        });
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(SimpleScore.of(-10L),
                SimpleScore.of(-10L));
        PlannerAssert.assertObjectsAreNotEqual(SimpleScore.of(-10L),
                SimpleScore.of(-30L));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                SimpleScore.of(Integer.MIN_VALUE - 4000L),
                SimpleScore.of(-300L),
                SimpleScore.of(-20L),
                SimpleScore.of(-1L),
                SimpleScore.of(0L),
                SimpleScore.of(1L),
                SimpleScore.of(Integer.MAX_VALUE + 4000L));
    }
}
