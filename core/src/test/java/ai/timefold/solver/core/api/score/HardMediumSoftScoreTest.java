package ai.timefold.solver.core.api.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.testutil.PlannerAssert;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class HardMediumSoftScoreTest extends AbstractScoreTest {

    @Test
    void of() {
        assertThat(HardMediumSoftScore.ofHard(-147L)).isEqualTo(HardMediumSoftScore.of(-147L, 0L, 0L));
        assertThat(HardMediumSoftScore.ofMedium(-258L)).isEqualTo(HardMediumSoftScore.of(0L, -258L, 0L));
        assertThat(HardMediumSoftScore.ofSoft(-369L)).isEqualTo(HardMediumSoftScore.of(0L, 0L, -369L));
    }

    @Test
    void parseScore() {
        assertThat(HardMediumSoftScore.parseScore("-147hard/-258medium/-369soft"))
                .isEqualTo(HardMediumSoftScore.of(-147L, -258L, -369L));
        assertThat(HardMediumSoftScore.parseScore("-147hard/-258medium/*soft"))
                .isEqualTo(HardMediumSoftScore.of(-147L, -258L, Long.MIN_VALUE));
        assertThat(HardMediumSoftScore.parseScore("-147hard/*medium/-369soft"))
                .isEqualTo(HardMediumSoftScore.of(-147L, Long.MIN_VALUE, -369L));
    }

    @Test
    void toShortString() {
        assertThat(HardMediumSoftScore.of(0L, 0L, 0L).toShortString()).isEqualTo("0");
        assertThat(HardMediumSoftScore.of(0L, 0L, -369L).toShortString()).isEqualTo("-369soft");
        assertThat(HardMediumSoftScore.of(0L, -258L, 0L).toShortString()).isEqualTo("-258medium");
        assertThat(HardMediumSoftScore.of(0L, -258L, -369L).toShortString()).isEqualTo("-258medium/-369soft");
        assertThat(HardMediumSoftScore.of(-147L, -258L, -369L).toShortString()).isEqualTo("-147hard/-258medium/-369soft");
    }

    @Test
    void testToString() {
        assertThat(HardMediumSoftScore.of(0L, -258L, -369L)).hasToString("0hard/-258medium/-369soft");
        assertThat(HardMediumSoftScore.of(-147L, -258L, -369L)).hasToString("-147hard/-258medium/-369soft");
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> HardMediumSoftScore.parseScore("-147"));
    }

    @Test
    void feasible() {
        assertScoreNotFeasible(HardMediumSoftScore.of(-5L, -300L, -4000L));
        assertScoreFeasible(HardMediumSoftScore.of(0L, -300L, -4000L),
                HardMediumSoftScore.of(2L, -300L, -4000L));
    }

    @Test
    void add() {
        assertThat(HardMediumSoftScore.of(20L, -20L, -4000L).add(
                HardMediumSoftScore.of(-1L, -300L, 4000L))).isEqualTo(HardMediumSoftScore.of(19L, -320L, 0L));
    }

    @Test
    void subtract() {
        assertThat(HardMediumSoftScore.of(20L, -20L, -4000L).subtract(
                HardMediumSoftScore.of(-1L, -300L, 4000L))).isEqualTo(HardMediumSoftScore.of(21L, 280L, -8000L));
    }

    @Test
    void multiply() {
        assertThat(HardMediumSoftScore.of(5L, -5L, 5L).multiply(1.2)).isEqualTo(HardMediumSoftScore.of(6L, -6L, 6L));
        assertThat(HardMediumSoftScore.of(1L, -1L, 1L).multiply(1.2)).isEqualTo(HardMediumSoftScore.of(1L, -2L, 1L));
        assertThat(HardMediumSoftScore.of(4L, -4L, 4L).multiply(1.2)).isEqualTo(HardMediumSoftScore.of(4L, -5L, 4L));
    }

    @Test
    void divide() {
        assertThat(HardMediumSoftScore.of(25L, -25L, 25L).divide(5.0)).isEqualTo(HardMediumSoftScore.of(5L, -5L, 5L));
        assertThat(HardMediumSoftScore.of(21L, -21L, 21L).divide(5.0)).isEqualTo(HardMediumSoftScore.of(4L, -5L, 4L));
        assertThat(HardMediumSoftScore.of(24L, -24L, 24L).divide(5.0)).isEqualTo(HardMediumSoftScore.of(4L, -5L, 4L));
    }

    @Test
    void power() {
        assertThat(HardMediumSoftScore.of(3L, -4L, 5L).power(2.0)).isEqualTo(HardMediumSoftScore.of(9L, 16L, 25L));
        assertThat(HardMediumSoftScore.of(9L, 16L, 25L).power(0.5)).isEqualTo(HardMediumSoftScore.of(3L, 4L, 5L));
    }

    @Test
    void negate() {
        assertThat(HardMediumSoftScore.of(3L, -4L, 5L).negate()).isEqualTo(HardMediumSoftScore.of(-3L, 4L, -5L));
        assertThat(HardMediumSoftScore.of(-3L, 4L, -5L).negate()).isEqualTo(HardMediumSoftScore.of(3L, -4L, 5L));
    }

    @Test
    void abs() {
        assertThat(HardMediumSoftScore.of(3L, 4L, 5L).abs()).isEqualTo(HardMediumSoftScore.of(3L, 4L, 5L));
        assertThat(HardMediumSoftScore.of(3L, -4L, 5L).abs()).isEqualTo(HardMediumSoftScore.of(3L, 4L, 5L));
        assertThat(HardMediumSoftScore.of(-3L, 4L, -5L).abs()).isEqualTo(HardMediumSoftScore.of(3L, 4L, 5L));
        assertThat(HardMediumSoftScore.of(-3L, -4L, -5L).abs()).isEqualTo(HardMediumSoftScore.of(3L, 4L, 5L));
    }

    @Test
    void zero() {
        HardMediumSoftScore manualZero = HardMediumSoftScore.of(0, 0, 0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(manualZero.zero()).isEqualTo(manualZero);
            softly.assertThat(manualZero.isZero()).isTrue();
            HardMediumSoftScore manualOne = HardMediumSoftScore.of(0, 0, 1);
            softly.assertThat(manualOne.isZero()).isFalse();
        });
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(
                HardMediumSoftScore.of(-10L, -200L, -3000L),
                HardMediumSoftScore.of(-10L, -200L, -3000L));
        PlannerAssert.assertObjectsAreNotEqual(
                HardMediumSoftScore.of(-10L, -200L, -3000L),
                HardMediumSoftScore.of(-30L, -200L, -3000L),
                HardMediumSoftScore.of(-10L, -400L, -3000L),
                HardMediumSoftScore.of(-10L, -400L, -5000L));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                HardMediumSoftScore.of(-20L, Long.MIN_VALUE, Long.MIN_VALUE),
                HardMediumSoftScore.of(-20L, Long.MIN_VALUE, -20L),
                HardMediumSoftScore.of(-20L, Long.MIN_VALUE, 1L),
                HardMediumSoftScore.of(-20L, -300L, -4000L),
                HardMediumSoftScore.of(-20L, -300L, -300L),
                HardMediumSoftScore.of(-20L, -300L, -20L),
                HardMediumSoftScore.of(-20L, -300L, 300L),
                HardMediumSoftScore.of(-20L, -20L, -300L),
                HardMediumSoftScore.of(-20L, -20L, 0L),
                HardMediumSoftScore.of(-20L, -20L, 1L),
                HardMediumSoftScore.of(-1L, -300L, -4000L),
                HardMediumSoftScore.of(-1L, -300L, -20L),
                HardMediumSoftScore.of(-1L, -20L, -300L),
                HardMediumSoftScore.of(1L, Long.MIN_VALUE, -20L),
                HardMediumSoftScore.of(1L, -20L, Long.MIN_VALUE));
    }
}
