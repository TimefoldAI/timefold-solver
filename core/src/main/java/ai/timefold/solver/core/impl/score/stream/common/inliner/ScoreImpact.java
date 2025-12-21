package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.Score;

public interface ScoreImpact<Score_ extends Score<Score_>> {

    AbstractScoreInliner<Score_> scoreInliner();

    void undo();

    Score_ toScore();

}
