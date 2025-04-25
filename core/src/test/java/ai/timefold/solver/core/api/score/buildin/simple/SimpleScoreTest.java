package ai.timefold.solver.core.api.score.buildin.simple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.AbstractScoreTest;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class SimpleScoreTest extends AbstractScoreTest {

    @Test
    void parseScore() {
        assertThat(SimpleScore.parseScore("-147")).isEqualTo(SimpleScore.of(-147));
        assertThat(SimpleScore.parseScore("*")).isEqualTo(SimpleScore.of(Integer.MIN_VALUE));
    }

    @Test
    void toShortString() {
        assertThat(SimpleScore.of(0).toShortString()).isEqualTo("0");
        assertThat(SimpleScore.of(-147).toShortString()).isEqualTo("-147");
    }

    @Test
    void testToString() {
        assertThat(SimpleScore.of(0)).hasToString("0");
        assertThat(SimpleScore.of(-147)).hasToString("-147");
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> SimpleScore.parseScore("-147hard/-258soft"));
    }

    @Test
    void add() {
        assertThat(SimpleScore.of(20).add(
                SimpleScore.of(-1))).isEqualTo(SimpleScore.of(19));
    }

    @Test
    void subtract() {
        assertThat(SimpleScore.of(20).subtract(
                SimpleScore.of(-1))).isEqualTo(SimpleScore.of(21));
    }

    @Test
    void multiply() {
        assertThat(SimpleScore.of(5).multiply(1.2)).isEqualTo(SimpleScore.of(6));
        assertThat(SimpleScore.of(1).multiply(1.2)).isEqualTo(SimpleScore.of(1));
        assertThat(SimpleScore.of(4).multiply(1.2)).isEqualTo(SimpleScore.of(4));
    }

    @Test
    void divide() {
        assertThat(SimpleScore.of(25).divide(5.0)).isEqualTo(SimpleScore.of(5));
        assertThat(SimpleScore.of(21).divide(5.0)).isEqualTo(SimpleScore.of(4));
        assertThat(SimpleScore.of(24).divide(5.0)).isEqualTo(SimpleScore.of(4));
    }

    @Test
    void power() {
        assertThat(SimpleScore.of(5).power(2.0)).isEqualTo(SimpleScore.of(25));
        assertThat(SimpleScore.of(25).power(0.5)).isEqualTo(SimpleScore.of(5));
    }

    @Test
    void negate() {
        assertThat(SimpleScore.of(5).negate()).isEqualTo(SimpleScore.of(-5));
        assertThat(SimpleScore.of(-5).negate()).isEqualTo(SimpleScore.of(5));
    }

    @Test
    void abs() {
        assertThat(SimpleScore.of(5).abs()).isEqualTo(SimpleScore.of(5));
        assertThat(SimpleScore.of(-5).abs()).isEqualTo(SimpleScore.of(5));
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
        PlannerAssert.assertObjectsAreEqual(SimpleScore.of(-10),
                SimpleScore.of(-10));
        PlannerAssert.assertObjectsAreNotEqual(SimpleScore.of(-10),
                SimpleScore.of(-30));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                SimpleScore.of(-300),
                SimpleScore.of(-20),
                SimpleScore.of(-1),
                SimpleScore.of(0),
                SimpleScore.of(1));
    }
}
