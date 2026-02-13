package ai.timefold.solver.core.api.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.testutil.PlannerAssert;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class HardSoftScoreTest extends AbstractScoreTest {

    @Test
    void of() {
        assertThat(HardSoftScore.ofHard(-147L)).isEqualTo(HardSoftScore.of(-147L, 0L));
        assertThat(HardSoftScore.ofSoft(-258L)).isEqualTo(HardSoftScore.of(0L, -258L));
    }

    @Test
    void parseScore() {
        assertThat(HardSoftScore.parseScore("-147hard/-258soft")).isEqualTo(HardSoftScore.of(-147L, -258L));
        assertThat(HardSoftScore.parseScore("-147hard/*soft")).isEqualTo(HardSoftScore.of(-147L, Long.MIN_VALUE));
    }

    @Test
    void toShortString() {
        assertThat(HardSoftScore.of(0L, 0L).toShortString()).isEqualTo("0");
        assertThat(HardSoftScore.of(0L, -258L).toShortString()).isEqualTo("-258soft");
        assertThat(HardSoftScore.of(-147L, 0L).toShortString()).isEqualTo("-147hard");
        assertThat(HardSoftScore.of(-147L, -258L).toShortString()).isEqualTo("-147hard/-258soft");
    }

    @Test
    void testToString() {
        assertThat(HardSoftScore.of(0L, -258L)).hasToString("0hard/-258soft");
        assertThat(HardSoftScore.of(-147L, -258L)).hasToString("-147hard/-258soft");
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> HardSoftScore.parseScore("-147"));
    }

    @Test
    void feasible() {
        assertScoreNotFeasible(HardSoftScore.of(-5L, -300L));
        assertScoreFeasible(HardSoftScore.of(0L, -300L),
                HardSoftScore.of(2L, -300L));
    }

    @Test
    void add() {
        assertThat(HardSoftScore.of(20L, -20L).add(
                HardSoftScore.of(-1L, -300L))).isEqualTo(HardSoftScore.of(19L, -320L));
    }

    @Test
    void subtract() {
        assertThat(HardSoftScore.of(20L, -20L).subtract(
                HardSoftScore.of(-1L, -300L))).isEqualTo(HardSoftScore.of(21L, 280L));
    }

    @Test
    void multiply() {
        assertThat(HardSoftScore.of(5L, -5L).multiply(1.2)).isEqualTo(HardSoftScore.of(6L, -6L));
        assertThat(HardSoftScore.of(1L, -1L).multiply(1.2)).isEqualTo(HardSoftScore.of(1L, -2L));
        assertThat(HardSoftScore.of(4L, -4L).multiply(1.2)).isEqualTo(HardSoftScore.of(4L, -5L));
    }

    @Test
    void divide() {
        assertThat(HardSoftScore.of(25L, -25L).divide(5.0)).isEqualTo(HardSoftScore.of(5L, -5L));
        assertThat(HardSoftScore.of(21L, -21L).divide(5.0)).isEqualTo(HardSoftScore.of(4L, -5L));
        assertThat(HardSoftScore.of(24L, -24L).divide(5.0)).isEqualTo(HardSoftScore.of(4L, -5L));
    }

    @Test
    void power() {
        assertThat(HardSoftScore.of(-4L, 5L).power(2.0)).isEqualTo(HardSoftScore.of(16L, 25L));
        assertThat(HardSoftScore.of(16L, 25L).power(0.5)).isEqualTo(HardSoftScore.of(4L, 5L));
    }

    @Test
    void negate() {
        assertThat(HardSoftScore.of(4L, -5L).negate()).isEqualTo(HardSoftScore.of(-4L, 5L));
        assertThat(HardSoftScore.of(-4L, 5L).negate()).isEqualTo(HardSoftScore.of(4L, -5L));
    }

    @Test
    void abs() {
        assertThat(HardSoftScore.of(4L, 5L).abs()).isEqualTo(HardSoftScore.of(4L, 5L));
        assertThat(HardSoftScore.of(4L, -5L).abs()).isEqualTo(HardSoftScore.of(4L, 5L));
        assertThat(HardSoftScore.of(-4L, 5L).abs()).isEqualTo(HardSoftScore.of(4L, 5L));
        assertThat(HardSoftScore.of(-4L, -5L).abs()).isEqualTo(HardSoftScore.of(4L, 5L));
    }

    @Test
    void zero() {
        HardSoftScore manualZero = HardSoftScore.of(0, 0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(manualZero.zero()).isEqualTo(manualZero);
            softly.assertThat(manualZero.isZero()).isTrue();
            HardSoftScore manualOne = HardSoftScore.of(0, 1);
            softly.assertThat(manualOne.isZero()).isFalse();
        });
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(HardSoftScore.of(-10L, -200L),
                HardSoftScore.of(-10L, -200L));
        PlannerAssert.assertObjectsAreNotEqual(
                HardSoftScore.of(-10L, -200L),
                HardSoftScore.of(-30L, -200L),
                HardSoftScore.of(-10L, -400L));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                HardSoftScore.of(-20L, Long.MIN_VALUE),
                HardSoftScore.of(-20L, -20L),
                HardSoftScore.of(-1L, -300L),
                HardSoftScore.of(-1L, 4000L),
                HardSoftScore.of(0L, -1L),
                HardSoftScore.of(0L, 0L),
                HardSoftScore.of(0L, 1L));
    }
}
