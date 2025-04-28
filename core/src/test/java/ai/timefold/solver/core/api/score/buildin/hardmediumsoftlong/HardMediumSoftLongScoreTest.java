package ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.AbstractScoreTest;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class HardMediumSoftLongScoreTest extends AbstractScoreTest {

    @Test
    void of() {
        assertThat(HardMediumSoftLongScore.ofHard(-147L)).isEqualTo(HardMediumSoftLongScore.of(-147L, 0L, 0L));
        assertThat(HardMediumSoftLongScore.ofMedium(-258L)).isEqualTo(HardMediumSoftLongScore.of(0L, -258L, 0L));
        assertThat(HardMediumSoftLongScore.ofSoft(-369L)).isEqualTo(HardMediumSoftLongScore.of(0L, 0L, -369L));
    }

    @Test
    void parseScore() {
        assertThat(HardMediumSoftLongScore.parseScore("-147hard/-258medium/-369soft"))
                .isEqualTo(HardMediumSoftLongScore.of(-147L, -258L, -369L));
        assertThat(HardMediumSoftLongScore.parseScore("-147hard/-258medium/*soft"))
                .isEqualTo(HardMediumSoftLongScore.of(-147L, -258L, Long.MIN_VALUE));
        assertThat(HardMediumSoftLongScore.parseScore("-147hard/*medium/-369soft"))
                .isEqualTo(HardMediumSoftLongScore.of(-147L, Long.MIN_VALUE, -369L));
    }

    @Test
    void toShortString() {
        assertThat(HardMediumSoftLongScore.of(0L, 0L, 0L).toShortString()).isEqualTo("0");
        assertThat(HardMediumSoftLongScore.of(0L, 0L, -369L).toShortString()).isEqualTo("-369soft");
        assertThat(HardMediumSoftLongScore.of(0L, -258L, 0L).toShortString()).isEqualTo("-258medium");
        assertThat(HardMediumSoftLongScore.of(0L, -258L, -369L).toShortString()).isEqualTo("-258medium/-369soft");
        assertThat(HardMediumSoftLongScore.of(-147L, -258L, -369L).toShortString()).isEqualTo("-147hard/-258medium/-369soft");
    }

    @Test
    void testToString() {
        assertThat(HardMediumSoftLongScore.of(0L, -258L, -369L)).hasToString("0hard/-258medium/-369soft");
        assertThat(HardMediumSoftLongScore.of(-147L, -258L, -369L)).hasToString("-147hard/-258medium/-369soft");
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> HardMediumSoftLongScore.parseScore("-147"));
    }

    @Test
    void feasible() {
        assertScoreNotFeasible(HardMediumSoftLongScore.of(-5L, -300L, -4000L));
        assertScoreFeasible(HardMediumSoftLongScore.of(0L, -300L, -4000L),
                HardMediumSoftLongScore.of(2L, -300L, -4000L));
    }

    @Test
    void add() {
        assertThat(HardMediumSoftLongScore.of(20L, -20L, -4000L).add(
                HardMediumSoftLongScore.of(-1L, -300L, 4000L))).isEqualTo(HardMediumSoftLongScore.of(19L, -320L, 0L));
    }

    @Test
    void subtract() {
        assertThat(HardMediumSoftLongScore.of(20L, -20L, -4000L).subtract(
                HardMediumSoftLongScore.of(-1L, -300L, 4000L))).isEqualTo(HardMediumSoftLongScore.of(21L, 280L, -8000L));
    }

    @Test
    void multiply() {
        assertThat(HardMediumSoftLongScore.of(5L, -5L, 5L).multiply(1.2)).isEqualTo(HardMediumSoftLongScore.of(6L, -6L, 6L));
        assertThat(HardMediumSoftLongScore.of(1L, -1L, 1L).multiply(1.2)).isEqualTo(HardMediumSoftLongScore.of(1L, -2L, 1L));
        assertThat(HardMediumSoftLongScore.of(4L, -4L, 4L).multiply(1.2)).isEqualTo(HardMediumSoftLongScore.of(4L, -5L, 4L));
    }

    @Test
    void divide() {
        assertThat(HardMediumSoftLongScore.of(25L, -25L, 25L).divide(5.0)).isEqualTo(HardMediumSoftLongScore.of(5L, -5L, 5L));
        assertThat(HardMediumSoftLongScore.of(21L, -21L, 21L).divide(5.0)).isEqualTo(HardMediumSoftLongScore.of(4L, -5L, 4L));
        assertThat(HardMediumSoftLongScore.of(24L, -24L, 24L).divide(5.0)).isEqualTo(HardMediumSoftLongScore.of(4L, -5L, 4L));
    }

    @Test
    void power() {
        assertThat(HardMediumSoftLongScore.of(3L, -4L, 5L).power(2.0)).isEqualTo(HardMediumSoftLongScore.of(9L, 16L, 25L));
        assertThat(HardMediumSoftLongScore.of(9L, 16L, 25L).power(0.5)).isEqualTo(HardMediumSoftLongScore.of(3L, 4L, 5L));
    }

    @Test
    void negate() {
        assertThat(HardMediumSoftLongScore.of(3L, -4L, 5L).negate()).isEqualTo(HardMediumSoftLongScore.of(-3L, 4L, -5L));
        assertThat(HardMediumSoftLongScore.of(-3L, 4L, -5L).negate()).isEqualTo(HardMediumSoftLongScore.of(3L, -4L, 5L));
    }

    @Test
    void abs() {
        assertThat(HardMediumSoftLongScore.of(3L, 4L, 5L).abs()).isEqualTo(HardMediumSoftLongScore.of(3L, 4L, 5L));
        assertThat(HardMediumSoftLongScore.of(3L, -4L, 5L).abs()).isEqualTo(HardMediumSoftLongScore.of(3L, 4L, 5L));
        assertThat(HardMediumSoftLongScore.of(-3L, 4L, -5L).abs()).isEqualTo(HardMediumSoftLongScore.of(3L, 4L, 5L));
        assertThat(HardMediumSoftLongScore.of(-3L, -4L, -5L).abs()).isEqualTo(HardMediumSoftLongScore.of(3L, 4L, 5L));
    }

    @Test
    void zero() {
        HardMediumSoftLongScore manualZero = HardMediumSoftLongScore.of(0, 0, 0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(manualZero.zero()).isEqualTo(manualZero);
            softly.assertThat(manualZero.isZero()).isTrue();
            HardMediumSoftLongScore manualOne = HardMediumSoftLongScore.of(0, 0, 1);
            softly.assertThat(manualOne.isZero()).isFalse();
        });
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(
                HardMediumSoftLongScore.of(-10L, -200L, -3000L),
                HardMediumSoftLongScore.of(-10L, -200L, -3000L));
        PlannerAssert.assertObjectsAreNotEqual(
                HardMediumSoftLongScore.of(-10L, -200L, -3000L),
                HardMediumSoftLongScore.of(-30L, -200L, -3000L),
                HardMediumSoftLongScore.of(-10L, -400L, -3000L),
                HardMediumSoftLongScore.of(-10L, -400L, -5000L));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                HardMediumSoftLongScore.of(-20L, Long.MIN_VALUE, Long.MIN_VALUE),
                HardMediumSoftLongScore.of(-20L, Long.MIN_VALUE, -20L),
                HardMediumSoftLongScore.of(-20L, Long.MIN_VALUE, 1L),
                HardMediumSoftLongScore.of(-20L, -300L, -4000L),
                HardMediumSoftLongScore.of(-20L, -300L, -300L),
                HardMediumSoftLongScore.of(-20L, -300L, -20L),
                HardMediumSoftLongScore.of(-20L, -300L, 300L),
                HardMediumSoftLongScore.of(-20L, -20L, -300L),
                HardMediumSoftLongScore.of(-20L, -20L, 0L),
                HardMediumSoftLongScore.of(-20L, -20L, 1L),
                HardMediumSoftLongScore.of(-1L, -300L, -4000L),
                HardMediumSoftLongScore.of(-1L, -300L, -20L),
                HardMediumSoftLongScore.of(-1L, -20L, -300L),
                HardMediumSoftLongScore.of(1L, Long.MIN_VALUE, -20L),
                HardMediumSoftLongScore.of(1L, -20L, Long.MIN_VALUE));
    }
}
