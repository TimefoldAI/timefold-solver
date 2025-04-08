package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.junit.jupiter.api.Test;

class HardSoftScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new HardSoftScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(HardSoftScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new HardSoftScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(HardSoftScore.ONE_SOFT);
    }

    @Test
    void getLevelsSize() {
        assertThat(new HardSoftScoreDefinition().getLevelsSize()).isEqualTo(2);
    }

    @Test
    void getLevelLabels() {
        assertThat(new HardSoftScoreDefinition().getLevelLabels()).containsExactly("hard score", "soft score");
    }

    @Test
    void getFeasibleLevelsSize() {
        assertThat(new HardSoftScoreDefinition().getFeasibleLevelsSize()).isEqualTo(1);
    }

    @Test
    void buildOptimisticBoundOnlyUp() {
        var scoreDefinition = new HardSoftScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 2),
                HardSoftScore.of(-1, -2));
        assertThat(optimisticBound.hardScore()).isEqualTo(Integer.MAX_VALUE);
        assertThat(optimisticBound.softScore()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void buildOptimisticBoundOnlyDown() {
        var scoreDefinition = new HardSoftScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 2),
                HardSoftScore.of(-1, -2));
        assertThat(optimisticBound.hardScore()).isEqualTo(-1);
        assertThat(optimisticBound.softScore()).isEqualTo(-2);
    }

    @Test
    void buildPessimisticBoundOnlyUp() {
        var scoreDefinition = new HardSoftScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 2),
                HardSoftScore.of(-1, -2));
        assertThat(pessimisticBound.hardScore()).isEqualTo(-1);
        assertThat(pessimisticBound.softScore()).isEqualTo(-2);
    }

    @Test
    void buildPessimisticBoundOnlyDown() {
        var scoreDefinition = new HardSoftScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 2),
                HardSoftScore.of(-1, -2));
        assertThat(pessimisticBound.hardScore()).isEqualTo(Integer.MIN_VALUE);
        assertThat(pessimisticBound.softScore()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new HardSoftScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(new Number[] { 0, 10 });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(new Number[] { 10, 10 });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(new Number[] { 0, 1 }));
    }

}
