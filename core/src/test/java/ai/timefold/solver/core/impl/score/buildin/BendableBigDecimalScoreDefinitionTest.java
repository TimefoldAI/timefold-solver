package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;

import org.junit.jupiter.api.Test;

class BendableBigDecimalScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new BendableBigDecimalScoreDefinition(1, 2).getZeroScore();
        assertThat(score).isEqualTo(BendableBigDecimalScore.zero(1, 2));
    }

    @Test
    void getSoftestOneScore() {
        var score = new BendableBigDecimalScoreDefinition(1, 2).getOneSoftestScore();
        assertThat(score).isEqualTo(BendableBigDecimalScore.of(new BigDecimal[] { BigDecimal.ZERO },
                new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ONE }));
    }

    @Test
    void getLevelsSize() {
        assertThat(new BendableBigDecimalScoreDefinition(1, 1).getLevelsSize()).isEqualTo(2);
        assertThat(new BendableBigDecimalScoreDefinition(3, 4).getLevelsSize()).isEqualTo(7);
        assertThat(new BendableBigDecimalScoreDefinition(4, 3).getLevelsSize()).isEqualTo(7);
        assertThat(new BendableBigDecimalScoreDefinition(0, 5).getLevelsSize()).isEqualTo(5);
        assertThat(new BendableBigDecimalScoreDefinition(5, 0).getLevelsSize()).isEqualTo(5);
    }

    @Test
    void getLevelLabels() {
        assertThat(new BendableBigDecimalScoreDefinition(1, 1).getLevelLabels()).containsExactly(
                "hard 0 score",
                "soft 0 score");
        assertThat(new BendableBigDecimalScoreDefinition(3, 4).getLevelLabels()).containsExactly(
                "hard 0 score", "hard 1 score", "hard 2 score",
                "soft 0 score", "soft 1 score", "soft 2 score", "soft 3 score");
        assertThat(new BendableBigDecimalScoreDefinition(4, 3).getLevelLabels()).containsExactly(
                "hard 0 score", "hard 1 score", "hard 2 score", "hard 3 score",
                "soft 0 score", "soft 1 score", "soft 2 score");
        assertThat(new BendableBigDecimalScoreDefinition(0, 5).getLevelLabels()).containsExactly(
                "soft 0 score", "soft 1 score", "soft 2 score", "soft 3 score", "soft 4 score");
        assertThat(new BendableBigDecimalScoreDefinition(5, 0).getLevelLabels()).containsExactly(
                "hard 0 score", "hard 1 score", "hard 2 score", "hard 3 score", "hard 4 score");
    }

    @Test
    void getFeasibleLevelsSize() {
        assertThat(new BendableBigDecimalScoreDefinition(1, 1).getFeasibleLevelsSize()).isEqualTo(1);
        assertThat(new BendableBigDecimalScoreDefinition(3, 4).getFeasibleLevelsSize()).isEqualTo(3);
        assertThat(new BendableBigDecimalScoreDefinition(4, 3).getFeasibleLevelsSize()).isEqualTo(4);
        assertThat(new BendableBigDecimalScoreDefinition(0, 5).getFeasibleLevelsSize()).isEqualTo(0);
        assertThat(new BendableBigDecimalScoreDefinition(5, 0).getFeasibleLevelsSize()).isEqualTo(5);
    }

    @Test
    void createScoreWithIllegalArgument() {
        var bendableScoreDefinition = new BendableBigDecimalScoreDefinition(2, 3);
        assertThatIllegalArgumentException().isThrownBy(() -> bendableScoreDefinition.createScore(
                new BigDecimal(1), new BigDecimal(2), new BigDecimal(3)));
    }

    @Test
    void createScore() {
        for (var hardLevelSize = 1; hardLevelSize < 5; hardLevelSize++) {
            for (var softLevelSize = 1; softLevelSize < 5; softLevelSize++) {
                var levelSize = hardLevelSize + softLevelSize;
                var scores = new BigDecimal[levelSize];
                for (var i = 0; i < levelSize; i++) {
                    scores[i] = new BigDecimal(i);
                }
                var bendableScoreDefinition = new BendableBigDecimalScoreDefinition(hardLevelSize,
                        softLevelSize);
                var bendableScore = bendableScoreDefinition.createScore(scores);
                assertThat(bendableScore.hardLevelsSize()).isEqualTo(hardLevelSize);
                assertThat(bendableScore.softLevelsSize()).isEqualTo(softLevelSize);
                for (var i = 0; i < levelSize; i++) {
                    if (i < hardLevelSize) {
                        assertThat(bendableScore.hardScore(i)).isEqualTo(scores[i]);
                    } else {
                        assertThat(bendableScore.softScore(i - hardLevelSize)).isEqualTo(scores[i]);
                    }
                }
            }
        }
    }

    // Optimistic and pessimistic bounds are currently not supported for this score definition

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new BendableBigDecimalScoreDefinition(1, 1);
        var dividend = scoreDefinition.createScore(BigDecimal.ZERO, BigDecimal.TEN);
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.createScore(BigDecimal.TEN, BigDecimal.TEN);
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.createScore(BigDecimal.ZERO, BigDecimal.ONE));
    }

}
