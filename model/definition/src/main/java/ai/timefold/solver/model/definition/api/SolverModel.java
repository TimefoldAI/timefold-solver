package ai.timefold.solver.model.definition.api;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;

public interface SolverModel<Score_ extends Score<Score_>> {

    Score_ getScore();

    ConstraintWeightOverrides<Score_> getConstraintWeightOverrides();
}
