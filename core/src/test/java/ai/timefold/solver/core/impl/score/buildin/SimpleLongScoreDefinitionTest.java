package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.junit.jupiter.api.Test;

class SimpleLongScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new SimpleLongScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(SimpleLongScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new SimpleLongScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(SimpleLongScore.ONE);
    }

    @Test
    void getLevelSize() {
        assertThat(new SimpleLongScoreDefinition().getLevelsSize()).isEqualTo(1);
    }

    @Test
    void getLevelLabels() {
        assertThat(new SimpleLongScoreDefinition().getLevelLabels()).containsExactly("score");
    }

    @Test
    void buildOptimisticBoundOnlyUp() {
        var scoreDefinition = new SimpleLongScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 1),
                SimpleLongScore.of(-1L));
        assertThat(optimisticBound.score()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void buildOptimisticBoundOnlyDown() {
        var scoreDefinition = new SimpleLongScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1),
                SimpleLongScore.of(-1L));
        assertThat(optimisticBound.score()).isEqualTo(-1L);
    }

    @Test
    void buildPessimisticBoundOnlyUp() {
        var scoreDefinition = new SimpleLongScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 1),
                SimpleLongScore.of(-1L));
        assertThat(pessimisticBound.score()).isEqualTo(-1L);
    }

    @Test
    void buildPessimisticBoundOnlyDown() {
        var scoreDefinition = new SimpleLongScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1),
                SimpleLongScore.of(-1L));
        assertThat(pessimisticBound.score()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new SimpleLongScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(new Number[] { 10L });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(new Number[] { 10L });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(new Number[] { 1L }));
    }

}
