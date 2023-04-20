package ai.timefold.solver.constraint.streams.common;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;

public abstract class AbstractConstraintBuilder<Score_ extends Score<Score_>> implements ConstraintBuilder {
    private final ConstraintConstructor constraintConstructor;
    private final ScoreImpactType impactType;
    private final Score_ constraintWeight;

    protected AbstractConstraintBuilder(ConstraintConstructor constraintConstructor, ScoreImpactType impactType,
            Score_ constraintWeight) {
        this.constraintConstructor = Objects.requireNonNull(constraintConstructor);
        this.impactType = Objects.requireNonNull(impactType);
        this.constraintWeight = constraintWeight;
    }

    protected abstract <JustificationMapping_> JustificationMapping_ getJustificationMapping();

    protected abstract <IndictedObjectsMapping_> IndictedObjectsMapping_ getIndictedObjectsMapping();

    @Override
    public final Constraint asConstraint(String constraintName) {
        return constraintConstructor.apply(null, constraintName, constraintWeight, impactType,
                getJustificationMapping(), getIndictedObjectsMapping());
    }

    @Override
    public final Constraint asConstraint(String constraintPackage, String constraintName) {
        return constraintConstructor.apply(constraintPackage, constraintName, constraintWeight, impactType,
                getJustificationMapping(), getIndictedObjectsMapping());
    }

}
