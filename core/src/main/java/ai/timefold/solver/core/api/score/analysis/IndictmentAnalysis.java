package ai.timefold.solver.core.api.score.analysis;

import java.util.List;

import ai.timefold.solver.core.api.score.Score;

public interface IndictmentAnalysis<Score_ extends Score<Score_>> {
    String type();
    Object indictee();
    Score_ score();
    List<MatchAnalysis<Score_>> matches();
}
