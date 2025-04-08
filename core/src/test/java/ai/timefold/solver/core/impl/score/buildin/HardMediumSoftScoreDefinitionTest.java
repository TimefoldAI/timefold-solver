package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.junit.jupiter.api.Test;

class HardMediumSoftScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new HardMediumSoftScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(HardMediumSoftScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new HardMediumSoftScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(HardMediumSoftScore.ONE_SOFT);
    }

    @Test
    void getLevelsSize() {
        assertThat(new HardMediumSoftScoreDefinition().getLevelsSize()).isEqualTo(3);
    }

    @Test
    void getLevelLabels() {
        assertThat(new HardMediumSoftScoreDefinition().getLevelLabels())
                .containsExactly("hard score", "medium score", "soft score");
    }

    @Test
    void getFeasibleLevelsSize() {
        assertThat(new HardMediumSoftScoreDefinition().getFeasibleLevelsSize()).isEqualTo(1);
    }

    @Test
    void buildOptimisticBoundOnlyUp() {
        var scoreDefinition = new HardMediumSoftScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 3),
                HardMediumSoftScore.of(-1, -2, -3));
        assertThat(optimisticBound.hardScore()).isEqualTo(Integer.MAX_VALUE);
        assertThat(optimisticBound.mediumScore()).isEqualTo(Integer.MAX_VALUE);
        assertThat(optimisticBound.softScore()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void buildOptimisticBoundOnlyDown() {
        var scoreDefinition = new HardMediumSoftScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 3),
                HardMediumSoftScore.of(-1, -2, -3));
        assertThat(optimisticBound.hardScore()).isEqualTo(-1);
        assertThat(optimisticBound.mediumScore()).isEqualTo(-2);
        assertThat(optimisticBound.softScore()).isEqualTo(-3);
    }

    @Test
    void buildPessimisticBoundOnlyUp() {
        var scoreDefinition = new HardMediumSoftScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 3),
                HardMediumSoftScore.of(-1, -2, -3));
        assertThat(pessimisticBound.hardScore()).isEqualTo(-1);
        assertThat(pessimisticBound.mediumScore()).isEqualTo(-2);
        assertThat(pessimisticBound.softScore()).isEqualTo(-3);
    }

    @Test
    void buildPessimisticBoundOnlyDown() {
        var scoreDefinition = new HardMediumSoftScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 3),
                HardMediumSoftScore.of(-1, -2, -3));
        assertThat(pessimisticBound.hardScore()).isEqualTo(Integer.MIN_VALUE);
        assertThat(pessimisticBound.mediumScore()).isEqualTo(Integer.MIN_VALUE);
        assertThat(pessimisticBound.softScore()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new HardMediumSoftScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(new Number[] { 0, 1, 10 });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(new Number[] { 10, 10, 10 });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(new Number[] { 0, 0, 1 }));
    }

}
