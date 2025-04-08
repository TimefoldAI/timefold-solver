package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.junit.jupiter.api.Test;

class BendableLongScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new BendableLongScoreDefinition(1, 2).getZeroScore();
        assertThat(score).isEqualTo(BendableLongScore.zero(1, 2));
    }

    @Test
    void getSoftestOneScore() {
        var score = new BendableLongScoreDefinition(1, 2).getOneSoftestScore();
        assertThat(score).isEqualTo(BendableLongScore.of(new long[1], new long[] { 0L, 1L }));
    }

    @Test
    void getLevelsSize() {
        assertThat(new BendableLongScoreDefinition(1, 1).getLevelsSize()).isEqualTo(2);
        assertThat(new BendableLongScoreDefinition(3, 4).getLevelsSize()).isEqualTo(7);
        assertThat(new BendableLongScoreDefinition(4, 3).getLevelsSize()).isEqualTo(7);
        assertThat(new BendableLongScoreDefinition(0, 5).getLevelsSize()).isEqualTo(5);
        assertThat(new BendableLongScoreDefinition(5, 0).getLevelsSize()).isEqualTo(5);
    }

    @Test
    void getLevelLabels() {
        assertThat(new BendableLongScoreDefinition(1, 1).getLevelLabels()).containsExactly(
                "hard 0 score",
                "soft 0 score");
        assertThat(new BendableLongScoreDefinition(3, 4).getLevelLabels()).containsExactly(
                "hard 0 score", "hard 1 score", "hard 2 score",
                "soft 0 score", "soft 1 score", "soft 2 score", "soft 3 score");
        assertThat(new BendableLongScoreDefinition(4, 3).getLevelLabels()).containsExactly(
                "hard 0 score", "hard 1 score", "hard 2 score", "hard 3 score",
                "soft 0 score", "soft 1 score", "soft 2 score");
        assertThat(new BendableLongScoreDefinition(0, 5).getLevelLabels()).containsExactly(
                "soft 0 score", "soft 1 score", "soft 2 score", "soft 3 score", "soft 4 score");
        assertThat(new BendableLongScoreDefinition(5, 0).getLevelLabels()).containsExactly(
                "hard 0 score", "hard 1 score", "hard 2 score", "hard 3 score", "hard 4 score");
    }

    @Test
    void getFeasibleLevelsSize() {
        assertThat(new BendableLongScoreDefinition(1, 1).getFeasibleLevelsSize()).isEqualTo(1);
        assertThat(new BendableLongScoreDefinition(3, 4).getFeasibleLevelsSize()).isEqualTo(3);
        assertThat(new BendableLongScoreDefinition(4, 3).getFeasibleLevelsSize()).isEqualTo(4);
        assertThat(new BendableLongScoreDefinition(0, 5).getFeasibleLevelsSize()).isEqualTo(0);
        assertThat(new BendableLongScoreDefinition(5, 0).getFeasibleLevelsSize()).isEqualTo(5);
    }

    @Test
    void createScoreWithIllegalArgument() {
        var bendableLongScoreDefinition = new BendableLongScoreDefinition(2, 3);
        assertThatIllegalArgumentException().isThrownBy(() -> bendableLongScoreDefinition.createScore(1, 2, 3));
    }

    @Test
    void createScore() {
        var hardLevelSize = 3;
        var softLevelSize = 2;
        var levelSize = hardLevelSize + softLevelSize;
        var scores = new long[levelSize];
        for (var i = 0; i < levelSize; i++) {
            scores[i] = ((long) Integer.MAX_VALUE) + i;
        }
        var bendableLongScoreDefinition = new BendableLongScoreDefinition(hardLevelSize, softLevelSize);
        var bendableLongScore = bendableLongScoreDefinition.createScore(scores);
        assertThat(bendableLongScore.hardLevelsSize()).isEqualTo(hardLevelSize);
        assertThat(bendableLongScore.softLevelsSize()).isEqualTo(softLevelSize);
        for (var i = 0; i < levelSize; i++) {
            if (i < hardLevelSize) {
                assertThat(bendableLongScore.hardScore(i)).isEqualTo(scores[i]);
            } else {
                assertThat(bendableLongScore.softScore(i - hardLevelSize)).isEqualTo(scores[i]);
            }
        }
    }

    @Test
    void buildOptimisticBoundOnlyUp() {
        var scoreDefinition = new BendableLongScoreDefinition(2, 3);
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 5),
                scoreDefinition.createScore(-1, -2, -3, -4, -5));
        assertThat(optimisticBound.hardScore(0)).isEqualTo(Long.MAX_VALUE);
        assertThat(optimisticBound.hardScore(1)).isEqualTo(Long.MAX_VALUE);
        assertThat(optimisticBound.softScore(0)).isEqualTo(Long.MAX_VALUE);
        assertThat(optimisticBound.softScore(1)).isEqualTo(Long.MAX_VALUE);
        assertThat(optimisticBound.softScore(2)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void buildOptimisticBoundOnlyDown() {
        var scoreDefinition = new BendableLongScoreDefinition(2, 3);
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 5),
                scoreDefinition.createScore(-1, -2, -3, -4, -5));
        assertThat(optimisticBound.hardScore(0)).isEqualTo(-1);
        assertThat(optimisticBound.hardScore(1)).isEqualTo(-2);
        assertThat(optimisticBound.softScore(0)).isEqualTo(-3);
        assertThat(optimisticBound.softScore(1)).isEqualTo(-4);
        assertThat(optimisticBound.softScore(2)).isEqualTo(-5);
    }

    @Test
    void buildPessimisticBoundOnlyUp() {
        var scoreDefinition = new BendableLongScoreDefinition(2, 3);
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 5),
                scoreDefinition.createScore(-1, -2, -3, -4, -5));
        assertThat(pessimisticBound.hardScore(0)).isEqualTo(-1);
        assertThat(pessimisticBound.hardScore(1)).isEqualTo(-2);
        assertThat(pessimisticBound.softScore(0)).isEqualTo(-3);
        assertThat(pessimisticBound.softScore(1)).isEqualTo(-4);
        assertThat(pessimisticBound.softScore(2)).isEqualTo(-5);
    }

    @Test
    void buildPessimisticBoundOnlyDown() {
        var scoreDefinition = new BendableLongScoreDefinition(2, 3);
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 5),
                scoreDefinition.createScore(-1, -2, -3, -4, -5));
        assertThat(pessimisticBound.hardScore(0)).isEqualTo(Long.MIN_VALUE);
        assertThat(pessimisticBound.hardScore(1)).isEqualTo(Long.MIN_VALUE);
        assertThat(pessimisticBound.softScore(0)).isEqualTo(Long.MIN_VALUE);
        assertThat(pessimisticBound.softScore(1)).isEqualTo(Long.MIN_VALUE);
        assertThat(pessimisticBound.softScore(2)).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new BendableLongScoreDefinition(1, 1);
        var dividend = scoreDefinition.createScore(0, 10);
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.createScore(0, 10));
    }

}
