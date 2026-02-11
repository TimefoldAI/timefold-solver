package ai.timefold.solver.benchmark.impl.statistic;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

public record ConstraintSummary<Score_ extends Score<Score_>>(ConstraintRef constraintRef, Score_ score, int count) {

}
