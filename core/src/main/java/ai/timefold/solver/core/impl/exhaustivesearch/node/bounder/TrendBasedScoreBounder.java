package ai.timefold.solver.core.impl.exhaustivesearch.node.bounder;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class TrendBasedScoreBounder implements ScoreBounder {

    protected final ScoreDefinition scoreDefinition;
    protected final InitializingScoreTrend initializingScoreTrend;

    public TrendBasedScoreBounder(ScoreDefinition scoreDefinition, InitializingScoreTrend initializingScoreTrend) {
        this.scoreDefinition = scoreDefinition;
        this.initializingScoreTrend = initializingScoreTrend;
    }

    @Override
    public Score calculateOptimisticBound(ScoreDirector scoreDirector, Score score) {
        return scoreDefinition.buildOptimisticBound(initializingScoreTrend, score);
    }

    @Override
    public Score calculatePessimisticBound(ScoreDirector scoreDirector, Score score) {
        return scoreDefinition.buildPessimisticBound(initializingScoreTrend, score);
    }

}
