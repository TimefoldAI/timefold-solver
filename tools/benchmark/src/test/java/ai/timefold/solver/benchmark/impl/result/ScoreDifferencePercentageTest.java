package ai.timefold.solver.benchmark.impl.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.data.Offset.offset;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

import org.junit.jupiter.api.Test;

class ScoreDifferencePercentageTest {

    @Test
    void calculateScoreDifferencePercentageException() {
        BendableScore score1 = BendableScore.of(new int[] { 1, 2, 3 }, new int[] { 4, 5 });
        BendableScore score2 = BendableScore.of(new int[] { 1, 2 }, new int[] { 4, 5 });
        assertThatIllegalStateException().isThrownBy(
                () -> ScoreDifferencePercentage.calculateScoreDifferencePercentage(score1, score2));
    }

    @Test
    void calculateScoreDifferencePercentage() {
        double tolerance = 0.00001;
        SimpleScore score1 = SimpleScore.of(-100);
        SimpleScore score2 = SimpleScore.of(-100);
        ScoreDifferencePercentage scoreDifferencePercentage = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(score1, score2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(0.0, offset(tolerance));

        score1 = SimpleScore.of(100);
        score2 = SimpleScore.of(100);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(score1, score2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(0.0, offset(tolerance));

        score1 = SimpleScore.of(-100);
        score2 = SimpleScore.of(-10);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(score1, score2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(0.9, offset(tolerance));

        score1 = SimpleScore.of(100);
        score2 = SimpleScore.of(10);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(score1, score2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(-0.9, offset(tolerance));

        score1 = SimpleScore.of(-100);
        score2 = SimpleScore.of(-1);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(score1, score2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(0.99, offset(tolerance));

        score1 = SimpleScore.of(100);
        score2 = SimpleScore.of(1);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(score1, score2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(-0.99, offset(tolerance));

        HardSoftScore hardSoftScore1 = HardSoftScore.of(-100, -1);
        HardSoftScore hardSoftScore2 = HardSoftScore.of(-100, -1);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(hardSoftScore1,
                hardSoftScore2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(0.0, offset(tolerance));
        assertThat(scoreDifferencePercentage.percentageLevels()[1]).isEqualTo(0.0, offset(tolerance));

        hardSoftScore1 = HardSoftScore.of(-100, -100);
        hardSoftScore2 = HardSoftScore.of(-1, -10);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(hardSoftScore1,
                hardSoftScore2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(0.99, offset(tolerance));
        assertThat(scoreDifferencePercentage.percentageLevels()[1]).isEqualTo(0.9, offset(tolerance));

        hardSoftScore1 = HardSoftScore.of(100, 100);
        hardSoftScore2 = HardSoftScore.of(1, 10);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(hardSoftScore1,
                hardSoftScore2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(-0.99, offset(tolerance));
        assertThat(scoreDifferencePercentage.percentageLevels()[1]).isEqualTo(-0.9, offset(tolerance));

        hardSoftScore1 = HardSoftScore.of(100, -100);
        hardSoftScore2 = HardSoftScore.of(-100, 200);
        scoreDifferencePercentage = ScoreDifferencePercentage.calculateScoreDifferencePercentage(hardSoftScore1,
                hardSoftScore2);
        assertThat(scoreDifferencePercentage.percentageLevels()[0]).isEqualTo(-2, offset(tolerance));
        assertThat(scoreDifferencePercentage.percentageLevels()[1]).isEqualTo(3, offset(tolerance));
    }

    @Test
    void add() {
        double tolerance = 0.00001;
        HardSoftScore hardSoftScore1 = HardSoftScore.of(-100, -1);
        HardSoftScore hardSoftScore2 = HardSoftScore.of(-200, -10);
        ScoreDifferencePercentage scoreDifferencePercentage = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(hardSoftScore1, hardSoftScore2);

        hardSoftScore1 = HardSoftScore.of(-100, -1);
        hardSoftScore2 = HardSoftScore.of(-200, -10);
        ScoreDifferencePercentage scoreDifferencePercentage2 = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(hardSoftScore1, hardSoftScore2);

        ScoreDifferencePercentage scoreDifferencePercentage1 = scoreDifferencePercentage.add(scoreDifferencePercentage2);
        double[] levels = scoreDifferencePercentage1.percentageLevels();
        assertThat(levels[0]).isEqualTo(-2.0, offset(tolerance));
        assertThat(levels[1]).isEqualTo(-18.0, offset(tolerance));
    }

    @Test
    void subtract() {
        double tolerance = 0.00001;
        HardSoftScore hardSoftScore1 = HardSoftScore.of(-100, -1);
        HardSoftScore hardSoftScore2 = HardSoftScore.of(-200, -10);
        ScoreDifferencePercentage scoreDifferencePercentage = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(hardSoftScore1, hardSoftScore2);

        hardSoftScore1 = HardSoftScore.of(-100, -1);
        hardSoftScore2 = HardSoftScore.of(-200, -10);
        ScoreDifferencePercentage scoreDifferencePercentage2 = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(hardSoftScore1, hardSoftScore2);

        ScoreDifferencePercentage scoreDifferencePercentage1 = scoreDifferencePercentage.subtract(scoreDifferencePercentage2);
        double[] levels = scoreDifferencePercentage1.percentageLevels();
        assertThat(levels[0]).isEqualTo(0.0, offset(tolerance));
        assertThat(levels[1]).isEqualTo(0.0, offset(tolerance));
    }

    @Test
    void multiply() {
        double tolerance = 0.00001;
        HardSoftScore hardSoftScore1 = HardSoftScore.of(-100, -1);
        HardSoftScore hardSoftScore2 = HardSoftScore.of(-200, -10);
        ScoreDifferencePercentage scoreDifferencePercentage = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(hardSoftScore1, hardSoftScore2);

        ScoreDifferencePercentage scoreDifferencePercentage2 = scoreDifferencePercentage.multiply(3.14);
        double[] levels = scoreDifferencePercentage2.percentageLevels();
        assertThat(levels[0]).isEqualTo(-3.14, offset(tolerance));
        assertThat(levels[1]).isEqualTo(-28.26, offset(tolerance));

        ScoreDifferencePercentage scoreDifferencePercentage1 = scoreDifferencePercentage.multiply(-1);
        levels = scoreDifferencePercentage1.percentageLevels();
        assertThat(levels[0]).isEqualTo(1, offset(tolerance));
        assertThat(levels[1]).isEqualTo(9.0, offset(tolerance));
    }

    @Test
    void divide() {
        double tolerance = 0.00001;
        HardSoftScore hardSoftScore1 = HardSoftScore.of(-100, -1);
        HardSoftScore hardSoftScore2 = HardSoftScore.of(-200, -10);
        ScoreDifferencePercentage scoreDifferencePercentage = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(hardSoftScore1, hardSoftScore2);

        ScoreDifferencePercentage scoreDifferencePercentage2 = scoreDifferencePercentage.multiply(0.5);
        double[] levels = scoreDifferencePercentage2.percentageLevels();
        assertThat(levels[0]).isEqualTo(-0.5, offset(tolerance));
        assertThat(levels[1]).isEqualTo(-4.5, offset(tolerance));

        ScoreDifferencePercentage scoreDifferencePercentage1 = scoreDifferencePercentage.multiply(-1);
        levels = scoreDifferencePercentage1.percentageLevels();
        assertThat(levels[0]).isEqualTo(1, offset(tolerance));
        assertThat(levels[1]).isEqualTo(9.0, offset(tolerance));
    }

    @Test
    void addWithWrongDimension() {
        HardSoftScore hardSoftScore1 = HardSoftScore.of(-100, -1);
        HardSoftScore hardSoftScore2 = HardSoftScore.of(-200, -10);
        ScoreDifferencePercentage scoreDifferencePercentage = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(hardSoftScore1, hardSoftScore2);

        SimpleScore score1 = SimpleScore.of(-100);
        SimpleScore score2 = SimpleScore.of(-200);
        ScoreDifferencePercentage scoreDifferencePercentage2 = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(score1, score2);

        assertThatIllegalStateException().isThrownBy(
                () -> scoreDifferencePercentage.add(scoreDifferencePercentage2));
    }

    @Test
    void subtractWithWrongDimension() {
        HardSoftScore hardSoftScore1 = HardSoftScore.of(-100, -1);
        HardSoftScore hardSoftScore2 = HardSoftScore.of(-200, -10);
        ScoreDifferencePercentage scoreDifferencePercentage = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(hardSoftScore1, hardSoftScore2);

        SimpleScore score1 = SimpleScore.of(-100);
        SimpleScore score2 = SimpleScore.of(-200);
        ScoreDifferencePercentage scoreDifferencePercentage2 = ScoreDifferencePercentage
                .calculateScoreDifferencePercentage(score1, score2);

        assertThatIllegalStateException().isThrownBy(
                () -> scoreDifferencePercentage.subtract(scoreDifferencePercentage2));
    }

}
