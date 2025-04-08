package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.junit.jupiter.api.Test;

class HardSoftLongScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new HardSoftLongScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(HardSoftLongScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new HardSoftLongScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(HardSoftLongScore.ONE_SOFT);
    }

    @Test
    void getLevelSize() {
        assertThat(new HardSoftLongScoreDefinition().getLevelsSize()).isEqualTo(2);
    }

    @Test
    void getLevelLabels() {
        assertThat(new HardSoftLongScoreDefinition().getLevelLabels()).containsExactly("hard score", "soft score");
    }

    @Test
    void getFeasibleLevelsSize() {
        assertThat(new HardSoftLongScoreDefinition().getFeasibleLevelsSize()).isEqualTo(1);
    }

    @Test
    void buildOptimisticBoundOnlyUp() {
        var scoreDefinition = new HardSoftLongScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 2),
                HardSoftLongScore.of(-1L, -2L));
        assertThat(optimisticBound.hardScore()).isEqualTo(Long.MAX_VALUE);
        assertThat(optimisticBound.softScore()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void buildOptimisticBoundOnlyDown() {
        var scoreDefinition = new HardSoftLongScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 2),
                HardSoftLongScore.of(-1L, -2L));
        assertThat(optimisticBound.hardScore()).isEqualTo(-1L);
        assertThat(optimisticBound.softScore()).isEqualTo(-2L);
    }

    @Test
    void buildPessimisticBoundOnlyUp() {
        var scoreDefinition = new HardSoftLongScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 2),
                HardSoftLongScore.of(-1L, -2L));
        assertThat(pessimisticBound.hardScore()).isEqualTo(-1L);
        assertThat(pessimisticBound.softScore()).isEqualTo(-2L);
    }

    @Test
    void buildPessimisticBoundOnlyDown() {
        var scoreDefinition = new HardSoftLongScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 2),
                HardSoftLongScore.of(-1L, -2L));
        assertThat(pessimisticBound.hardScore()).isEqualTo(Long.MIN_VALUE);
        assertThat(pessimisticBound.softScore()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new HardSoftLongScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(new Number[] { 0L, 10L });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(new Number[] { 10L, 10L });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(new Number[] { 0L, 1L }));
    }

}
