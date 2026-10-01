package ai.timefold.solver.core.impl.score.stream.common;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintMetadata;

@FunctionalInterface
public interface ConstraintConstructor<Score_ extends Score<Score_>, JustificationMapping_> {

    Constraint apply(ConstraintMetadata description, Score_ constraintWeight,
            ScoreImpactType impactType, JustificationMapping_ justificationMapping);

}
