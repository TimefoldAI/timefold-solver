package ai.timefold.solver.core.api.score.constraint;

import ai.timefold.solver.core.api.score.Score;

public record ConstraintMetaModel<Score_ extends Score<Score_>>(String constraintName, String constraintDescription,
        Score_ weight) {

}
