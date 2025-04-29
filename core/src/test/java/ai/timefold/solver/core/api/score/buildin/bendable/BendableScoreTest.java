package ai.timefold.solver.core.api.score.buildin.bendable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.AbstractScoreTest;
import ai.timefold.solver.core.impl.score.buildin.BendableScoreDefinition;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class BendableScoreTest extends AbstractScoreTest {

    private final BendableScoreDefinition scoreDefinitionHSS = new BendableScoreDefinition(1, 2);
    private final BendableScoreDefinition scoreDefinitionHHH = new BendableScoreDefinition(3, 0);
    private final BendableScoreDefinition scoreDefinitionSSS = new BendableScoreDefinition(0, 3);

    @Test
    void of() {
        assertThat(BendableScore.ofHard(1, 2, 0, -147)).isEqualTo(scoreDefinitionHSS.createScore(-147, 0, 0));
        assertThat(BendableScore.ofSoft(1, 2, 0, -258)).isEqualTo(scoreDefinitionHSS.createScore(0, -258, 0));
        assertThat(BendableScore.ofSoft(1, 2, 1, -369)).isEqualTo(scoreDefinitionHSS.createScore(0, 0, -369));
        assertThat(BendableScore.ofHard(3, 0, 2, -369)).isEqualTo(scoreDefinitionHHH.createScore(0, 0, -369));
        assertThat(BendableScore.ofSoft(0, 3, 2, -369)).isEqualTo(scoreDefinitionSSS.createScore(0, 0, -369));
    }

    @Test
    void parseScore() {
        assertThat(scoreDefinitionHSS.parseScore("[-147]hard/[-258/-369]soft"))
                .isEqualTo(scoreDefinitionHSS.createScore(-147, -258, -369));
        assertThat(scoreDefinitionHHH.parseScore("[-147/-258/-369]hard/[]soft"))
                .isEqualTo(scoreDefinitionHHH.createScore(-147, -258, -369));
        assertThat(scoreDefinitionSSS.parseScore("[]hard/[-147/-258/-369]soft"))
                .isEqualTo(scoreDefinitionSSS.createScore(-147, -258, -369));
        assertThat(scoreDefinitionHSS.parseScore("[-147]hard/[-258/*]soft"))
                .isEqualTo(scoreDefinitionHSS.createScore(-147, -258, Integer.MIN_VALUE));
        assertThat(scoreDefinitionHSS.parseScore("[-147]hard/[*/-369]soft"))
                .isEqualTo(scoreDefinitionHSS.createScore(-147, Integer.MIN_VALUE, -369));
    }

    @Test
    void toShortString() {
        assertThat(scoreDefinitionHSS.createScore(0, 0, 0).toShortString()).isEqualTo("0");
        assertThat(scoreDefinitionHSS.createScore(0, 0, -369).toShortString()).isEqualTo("[0/-369]soft");
        assertThat(scoreDefinitionHSS.createScore(0, -258, -369).toShortString()).isEqualTo("[-258/-369]soft");
        assertThat(scoreDefinitionHSS.createScore(-147, 0, 0).toShortString()).isEqualTo("[-147]hard");
        assertThat(scoreDefinitionHSS.createScore(-147, -258, -369).toShortString()).isEqualTo("[-147]hard/[-258/-369]soft");
        assertThat(scoreDefinitionHHH.createScore(-147, -258, -369).toShortString()).isEqualTo("[-147/-258/-369]hard");
        assertThat(scoreDefinitionSSS.createScore(-147, -258, -369).toShortString()).isEqualTo("[-147/-258/-369]soft");
    }

    @Test
    void testToString() {
        assertThat(scoreDefinitionHSS.createScore(0, -258, -369)).hasToString("[0]hard/[-258/-369]soft");
        assertThat(scoreDefinitionHSS.createScore(-147, -258, -369)).hasToString("[-147]hard/[-258/-369]soft");
        assertThat(scoreDefinitionHHH.createScore(-147, -258, -369)).hasToString("[-147/-258/-369]hard/[]soft");
        assertThat(scoreDefinitionSSS.createScore(-147, -258, -369)).hasToString("[]hard/[-147/-258/-369]soft");
        assertThat(new BendableScoreDefinition(0, 0).createScore()).hasToString("[]hard/[]soft");
    }

    @Test
    void parseScoreIllegalArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> scoreDefinitionHSS.parseScore("-147"));
    }

    @Test
    void getHardOrSoftScore() {
        BendableScore initializedScore = scoreDefinitionHSS.createScore(-5, -10, -200);
        assertThat(initializedScore.hardOrSoftScore(0)).isEqualTo(-5);
        assertThat(initializedScore.hardOrSoftScore(1)).isEqualTo(-10);
        assertThat(initializedScore.hardOrSoftScore(2)).isEqualTo(-200);
    }

    @Test
    void feasibleHSS() {
        assertScoreNotFeasible(scoreDefinitionHSS.createScore(-20, -300, -4000));
        assertScoreFeasible(scoreDefinitionHSS.createScore(0, -300, -4000),
                scoreDefinitionHSS.createScore(20, -300, -4000));
    }

    @Test
    void addHSS() {
        assertThat(scoreDefinitionHSS.createScore(20, -20, -4000).add(
                scoreDefinitionHSS.createScore(-1, -300, 4000))).isEqualTo(scoreDefinitionHSS.createScore(19, -320, 0));
    }

    @Test
    void subtractHSS() {
        assertThat(scoreDefinitionHSS.createScore(20, -20, -4000).subtract(
                scoreDefinitionHSS.createScore(-1, -300, 4000))).isEqualTo(scoreDefinitionHSS.createScore(21, 280, -8000));
    }

    @Test
    void multiplyHSS() {
        assertThat(scoreDefinitionHSS.createScore(5, -5, 5).multiply(1.2)).isEqualTo(scoreDefinitionHSS.createScore(6, -6, 6));
        assertThat(scoreDefinitionHSS.createScore(1, -1, 1).multiply(1.2)).isEqualTo(scoreDefinitionHSS.createScore(1, -2, 1));
        assertThat(scoreDefinitionHSS.createScore(4, -4, 4).multiply(1.2)).isEqualTo(scoreDefinitionHSS.createScore(4, -5, 4));
    }

    @Test
    void divideHSS() {
        assertThat(scoreDefinitionHSS.createScore(25, -25, 25).divide(5.0)).isEqualTo(scoreDefinitionHSS.createScore(5, -5, 5));
        assertThat(scoreDefinitionHSS.createScore(21, -21, 21).divide(5.0)).isEqualTo(scoreDefinitionHSS.createScore(4, -5, 4));
        assertThat(scoreDefinitionHSS.createScore(24, -24, 24).divide(5.0)).isEqualTo(scoreDefinitionHSS.createScore(4, -5, 4));
    }

    @Test
    void powerHSS() {
        assertThat(scoreDefinitionHSS.createScore(3, -4, 5).power(2.0)).isEqualTo(scoreDefinitionHSS.createScore(9, 16, 25));
        assertThat(scoreDefinitionHSS.createScore(9, 16, 25).power(0.5)).isEqualTo(scoreDefinitionHSS.createScore(3, 4, 5));
    }

    @Test
    void negateHSS() {
        assertThat(scoreDefinitionHSS.createScore(3, -4, 5).negate()).isEqualTo(scoreDefinitionHSS.createScore(-3, 4, -5));
        assertThat(scoreDefinitionHSS.createScore(-3, 4, -5).negate()).isEqualTo(scoreDefinitionHSS.createScore(3, -4, 5));
    }

    @Test
    void absHSS() {
        assertThat(scoreDefinitionHSS.createScore(3, 4, 5).abs()).isEqualTo(scoreDefinitionHSS.createScore(3, 4, 5));
        assertThat(scoreDefinitionHSS.createScore(3, -4, 5).abs()).isEqualTo(scoreDefinitionHSS.createScore(3, 4, 5));
        assertThat(scoreDefinitionHSS.createScore(-3, 4, -5).abs()).isEqualTo(scoreDefinitionHSS.createScore(3, 4, 5));
        assertThat(scoreDefinitionHSS.createScore(-3, -4, -5).abs()).isEqualTo(scoreDefinitionHSS.createScore(3, 4, 5));
    }

    @Test
    void zero() {
        BendableScore manualZero = BendableScore.zero(0, 1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(manualZero.zero()).isEqualTo(manualZero);
            softly.assertThat(manualZero.isZero()).isTrue();
            BendableScore manualOne = BendableScore.ofSoft(0, 1, 0, 1);
            softly.assertThat(manualOne.isZero()).isFalse();
        });
    }

    @Test
    void equalsAndHashCodeHSS() {
        PlannerAssert.assertObjectsAreEqual(scoreDefinitionHSS.createScore(-10, -200, -3000),
                scoreDefinitionHSS.createScore(-10, -200, -3000));
        PlannerAssert.assertObjectsAreNotEqual(scoreDefinitionHSS.createScore(-10, -200, -3000),
                scoreDefinitionHSS.createScore(-30, -200, -3000),
                scoreDefinitionHSS.createScore(-10, -400, -3000),
                scoreDefinitionHSS.createScore(-10, -400, -5000));
    }

    @Test
    void compareToHSS() {
        PlannerAssert.assertCompareToOrder(
                scoreDefinitionHSS.createScore(-20, Integer.MIN_VALUE, Integer.MIN_VALUE),
                scoreDefinitionHSS.createScore(-20, Integer.MIN_VALUE, -20),
                scoreDefinitionHSS.createScore(-20, Integer.MIN_VALUE, 1),
                scoreDefinitionHSS.createScore(-20, -300, -4000),
                scoreDefinitionHSS.createScore(-20, -300, -300),
                scoreDefinitionHSS.createScore(-20, -300, -20),
                scoreDefinitionHSS.createScore(-20, -300, 300),
                scoreDefinitionHSS.createScore(-20, -20, -300),
                scoreDefinitionHSS.createScore(-20, -20, 0),
                scoreDefinitionHSS.createScore(-20, -20, 1),
                scoreDefinitionHSS.createScore(-1, -300, -4000),
                scoreDefinitionHSS.createScore(-1, -300, -20),
                scoreDefinitionHSS.createScore(-1, -20, -300),
                scoreDefinitionHSS.createScore(1, Integer.MIN_VALUE, -20),
                scoreDefinitionHSS.createScore(1, -20, Integer.MIN_VALUE));
    }

    private final BendableScoreDefinition scoreDefinitionHHSSS = new BendableScoreDefinition(2, 3);

    @Test
    void feasibleHHSSS() {
        assertScoreNotFeasible(
                scoreDefinitionHHSSS.createScore(-1, -20, -300, -4000, -5000),
                scoreDefinitionHHSSS.createScore(-1, 0, -300, -4000, -5000),
                scoreDefinitionHHSSS.createScore(-1, 20, -300, -4000, -5000),
                scoreDefinitionHHSSS.createScore(0, -20, -300, -4000, -5000),
                scoreDefinitionHHSSS.createScore(1, -20, -300, -4000, -5000));
        assertScoreFeasible(
                scoreDefinitionHHSSS.createScore(0, 0, -300, -4000, -5000),
                scoreDefinitionHHSSS.createScore(0, 20, -300, -4000, -5000),
                scoreDefinitionHHSSS.createScore(1, 0, -300, -4000, -5000),
                scoreDefinitionHHSSS.createScore(1, 20, -300, -4000, -5000));
    }

    @Test
    void addHHSSS() {
        assertThat(scoreDefinitionHHSSS.createScore(20, -20, -4000, 0, 0).add(
                scoreDefinitionHHSSS.createScore(-1, -300, 4000, 0, 0)))
                .isEqualTo(scoreDefinitionHHSSS.createScore(19, -320, 0, 0, 0));
    }

    @Test
    void subtractHHSSS() {
        assertThat(scoreDefinitionHHSSS.createScore(20, -20, -4000, 0, 0).subtract(
                scoreDefinitionHHSSS.createScore(-1, -300, 4000, 0, 0)))
                .isEqualTo(scoreDefinitionHHSSS.createScore(21, 280, -8000, 0, 0));
    }

    @Test
    void multiplyHHSSS() {
        assertThat(scoreDefinitionHHSSS.createScore(5, -5, 5, 0, 0).multiply(1.2))
                .isEqualTo(scoreDefinitionHHSSS.createScore(6, -6, 6, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(1, -1, 1, 0, 0).multiply(1.2))
                .isEqualTo(scoreDefinitionHHSSS.createScore(1, -2, 1, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(4, -4, 4, 0, 0).multiply(1.2))
                .isEqualTo(scoreDefinitionHHSSS.createScore(4, -5, 4, 0, 0));
    }

    @Test
    void divideHHSSS() {
        assertThat(scoreDefinitionHHSSS.createScore(25, -25, 25, 0, 0).divide(5.0))
                .isEqualTo(scoreDefinitionHHSSS.createScore(5, -5, 5, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(21, -21, 21, 0, 0).divide(5.0))
                .isEqualTo(scoreDefinitionHHSSS.createScore(4, -5, 4, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(24, -24, 24, 0, 0).divide(5.0))
                .isEqualTo(scoreDefinitionHHSSS.createScore(4, -5, 4, 0, 0));
    }

    @Test
    void powerHHSSS() {
        assertThat(scoreDefinitionHHSSS.createScore(3, -4, 5, 0, 0).power(2.0))
                .isEqualTo(scoreDefinitionHHSSS.createScore(9, 16, 25, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(9, 16, 25, 0, 0).power(0.5))
                .isEqualTo(scoreDefinitionHHSSS.createScore(3, 4, 5, 0, 0));
    }

    @Test
    void negateHHSSS() {
        assertThat(scoreDefinitionHHSSS.createScore(3, -4, 5, 0, 0).negate())
                .isEqualTo(scoreDefinitionHHSSS.createScore(-3, 4, -5, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(-3, 4, -5, 0, 0).negate())
                .isEqualTo(scoreDefinitionHHSSS.createScore(3, -4, 5, 0, 0));
    }

    @Test
    void absHHSSS() {
        assertThat(scoreDefinitionHHSSS.createScore(3, 4, 5, 0, 0).abs())
                .isEqualTo(scoreDefinitionHHSSS.createScore(3, 4, 5, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(3, -4, 5, 0, 0).abs())
                .isEqualTo(scoreDefinitionHHSSS.createScore(3, 4, 5, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(-3, 4, -5, 0, 0).abs())
                .isEqualTo(scoreDefinitionHHSSS.createScore(3, 4, 5, 0, 0));
        assertThat(scoreDefinitionHHSSS.createScore(-3, -4, -5, 0, 0).abs())
                .isEqualTo(scoreDefinitionHHSSS.createScore(3, 4, 5, 0, 0));
    }

    @Test
    void equalsAndHashCodeHHSSS() {
        PlannerAssert.assertObjectsAreEqual(
                scoreDefinitionHHSSS.createScore(-10, -20, -30, 0, 0),
                scoreDefinitionHHSSS.createScore(-10, -20, -30, 0, 0));
    }

    @Test
    void compareToHHSSS() {
        PlannerAssert.assertCompareToOrder(
                scoreDefinitionHHSSS.createScore(-20, Integer.MIN_VALUE, Integer.MIN_VALUE, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, Integer.MIN_VALUE, -20, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, Integer.MIN_VALUE, 1, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, -300, -4000, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, -300, -300, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, -300, -20, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, -300, 300, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, -20, -300, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, -20, 0, 0, 0),
                scoreDefinitionHHSSS.createScore(-20, -20, 1, 0, 0),
                scoreDefinitionHHSSS.createScore(-1, -300, -4000, 0, 0),
                scoreDefinitionHHSSS.createScore(-1, -300, -20, 0, 0),
                scoreDefinitionHHSSS.createScore(-1, -20, -300, 0, 0),
                scoreDefinitionHHSSS.createScore(1, Integer.MIN_VALUE, -20, 0, 0),
                scoreDefinitionHHSSS.createScore(1, -20, Integer.MIN_VALUE, 0, 0));
    }
}
