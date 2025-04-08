package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.junit.jupiter.api.Test;

class HardMediumSoftLongScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new HardMediumSoftLongScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(HardMediumSoftLongScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new HardMediumSoftLongScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(HardMediumSoftLongScore.ONE_SOFT);
    }

    @Test
    void getLevelsSize() {
        assertThat(new HardMediumSoftLongScoreDefinition().getLevelsSize()).isEqualTo(3);
    }

    @Test
    void getLevelLabels() {
        assertThat(new HardMediumSoftLongScoreDefinition().getLevelLabels())
                .containsExactly("hard score", "medium score", "soft score");
    }

    @Test
    void getFeasibleLevelsSize() {
        assertThat(new HardMediumSoftLongScoreDefinition().getFeasibleLevelsSize()).isEqualTo(1);
    }

    @Test
    void buildOptimisticBoundOnlyUp() {
        var scoreDefinition = new HardMediumSoftLongScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 3),
                HardMediumSoftLongScore.of(-1L, -2L, -3L));
        assertThat(optimisticBound.hardScore()).isEqualTo(Long.MAX_VALUE);
        assertThat(optimisticBound.mediumScore()).isEqualTo(Long.MAX_VALUE);
        assertThat(optimisticBound.softScore()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void buildOptimisticBoundOnlyDown() {
        var scoreDefinition = new HardMediumSoftLongScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 3),
                HardMediumSoftLongScore.of(-1L, -2L, -3L));
        assertThat(optimisticBound.hardScore()).isEqualTo(-1L);
        assertThat(optimisticBound.mediumScore()).isEqualTo(-2L);
        assertThat(optimisticBound.softScore()).isEqualTo(-3L);
    }

    @Test
    void buildPessimisticBoundOnlyUp() {
        var scoreDefinition = new HardMediumSoftLongScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 3),
                HardMediumSoftLongScore.of(-1L, -2L, -3L));
        assertThat(pessimisticBound.hardScore()).isEqualTo(-1L);
        assertThat(pessimisticBound.mediumScore()).isEqualTo(-2L);
        assertThat(pessimisticBound.softScore()).isEqualTo(-3L);
    }

    @Test
    void buildPessimisticBoundOnlyDown() {
        var scoreDefinition = new HardMediumSoftLongScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 3),
                HardMediumSoftLongScore.of(-1L, -2L, -3L));
        assertThat(pessimisticBound.hardScore()).isEqualTo(Long.MIN_VALUE);
        assertThat(pessimisticBound.mediumScore()).isEqualTo(Long.MIN_VALUE);
        assertThat(pessimisticBound.softScore()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new HardMediumSoftLongScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(new Number[] { 0L, 1L, 10L });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(new Number[] { 10L, 10L, 10L });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(new Number[] { 0L, 0L, 1L }));
    }

}
