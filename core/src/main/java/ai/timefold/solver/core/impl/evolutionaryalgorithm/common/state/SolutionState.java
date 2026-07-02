package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public interface SolutionState<Solution_, Score_ extends Score<Score_>> {

    Solution_ getSolution();

    InnerScore<Score_> getScore();
}
