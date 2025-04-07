package ai.timefold.solver.core.impl.exhaustivesearch.node.bounder;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public final class TrendBasedScoreBounder<Score_ extends Score<Score_>> implements ScoreBounder<Score_> {

    private final ScoreDefinition<Score_> scoreDefinition;
    private final InitializingScoreTrend initializingScoreTrend;

    public TrendBasedScoreBounder(ScoreDefinition<Score_> scoreDefinition, InitializingScoreTrend initializingScoreTrend) {
        this.scoreDefinition = scoreDefinition;
        this.initializingScoreTrend = initializingScoreTrend;
    }

    @Override
    public InnerScore<Score_> calculateOptimisticBound(ScoreDirector<?> scoreDirector, InnerScore<Score_> score) {
        return new InnerScore<>(scoreDefinition.buildOptimisticBound(initializingScoreTrend, score.raw()), 0);
    }

    @Override
    public InnerScore<Score_> calculatePessimisticBound(ScoreDirector<?> scoreDirector, InnerScore<Score_> score) {
        return new InnerScore<>(scoreDefinition.buildPessimisticBound(initializingScoreTrend, score.raw()), 0);
    }

}
