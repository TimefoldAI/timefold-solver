package ai.timefold.solver.core.api.score.buildin.simplelong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.AbstractScoreTest;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class SimpleLongScoreTest extends AbstractScoreTest {

    @Test
    void parseScore() {
        assertThat(SimpleLongScore.parseScore("-147")).isEqualTo(SimpleLongScore.of(-147L));
        assertThat(SimpleLongScore.parseScore("*")).isEqualTo(SimpleLongScore.of(Long.MIN_VALUE));
    }

    @Test
    void toShortString() {
        assertThat(SimpleLongScore.of(0L).toShortString()).isEqualTo("0");
        assertThat(SimpleLongScore.of(-147L).toShortString()).isEqualTo("-147");
    }

    @Test
    void testToString() {
        assertThat(SimpleLongScore.of(0)).hasToString("0");
        assertThat(SimpleLongScore.of(-147L)).hasToString("-147");
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> SimpleLongScore.parseScore("-147hard/-258soft"));
    }

    @Test
    void add() {
        assertThat(SimpleLongScore.of(20L).add(
                SimpleLongScore.of(-1L))).isEqualTo(SimpleLongScore.of(19L));
    }

    @Test
    void subtract() {
        assertThat(SimpleLongScore.of(20L).subtract(
                SimpleLongScore.of(-1L))).isEqualTo(SimpleLongScore.of(21L));
    }

    @Test
    void multiply() {
        assertThat(SimpleLongScore.of(5L).multiply(1.2)).isEqualTo(SimpleLongScore.of(6L));
        assertThat(SimpleLongScore.of(1L).multiply(1.2)).isEqualTo(SimpleLongScore.of(1L));
        assertThat(SimpleLongScore.of(4L).multiply(1.2)).isEqualTo(SimpleLongScore.of(4L));
    }

    @Test
    void divide() {
        assertThat(SimpleLongScore.of(25L).divide(5.0)).isEqualTo(SimpleLongScore.of(5L));
        assertThat(SimpleLongScore.of(21L).divide(5.0)).isEqualTo(SimpleLongScore.of(4L));
        assertThat(SimpleLongScore.of(24L).divide(5.0)).isEqualTo(SimpleLongScore.of(4L));
    }

    @Test
    void power() {
        assertThat(SimpleLongScore.of(5L).power(2.0)).isEqualTo(SimpleLongScore.of(25L));
        assertThat(SimpleLongScore.of(25L).power(0.5)).isEqualTo(SimpleLongScore.of(5L));
    }

    @Test
    void negate() {
        assertThat(SimpleLongScore.of(5L).negate()).isEqualTo(SimpleLongScore.of(-5L));
        assertThat(SimpleLongScore.of(-5L).negate()).isEqualTo(SimpleLongScore.of(5L));
    }

    @Test
    void abs() {
        assertThat(SimpleLongScore.of(5L).abs()).isEqualTo(SimpleLongScore.of(5L));
        assertThat(SimpleLongScore.of(-5L).abs()).isEqualTo(SimpleLongScore.of(5L));
    }

    @Test
    void zero() {
        SimpleLongScore manualZero = SimpleLongScore.of(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(manualZero.zero()).isEqualTo(manualZero);
            softly.assertThat(manualZero.isZero()).isTrue();
            SimpleLongScore manualOne = SimpleLongScore.of(1);
            softly.assertThat(manualOne.isZero()).isFalse();
        });
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(SimpleLongScore.of(-10L),
                SimpleLongScore.of(-10L));
        PlannerAssert.assertObjectsAreNotEqual(SimpleLongScore.of(-10L),
                SimpleLongScore.of(-30L));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                SimpleLongScore.of(Integer.MIN_VALUE - 4000L),
                SimpleLongScore.of(-300L),
                SimpleLongScore.of(-20L),
                SimpleLongScore.of(-1L),
                SimpleLongScore.of(0L),
                SimpleLongScore.of(1L),
                SimpleLongScore.of(Integer.MAX_VALUE + 4000L));
    }
}
