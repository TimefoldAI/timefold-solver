package ai.timefold.solver.core.impl.score.stream.common;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;

@FunctionalInterface
public interface ConstraintConstructor<Score_ extends Score<Score_>, JustificationMapping_, IndictedObjectsMapping_> {

    Constraint apply(String constraintPackage, String constraintName, String constraintDescription, Score_ constraintWeight,
            ScoreImpactType impactType, JustificationMapping_ justificationMapping,
            IndictedObjectsMapping_ indictedObjectsMapping);

}
