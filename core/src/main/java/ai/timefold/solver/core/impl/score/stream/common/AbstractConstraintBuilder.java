package ai.timefold.solver.core.impl.score.stream.common;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;

@SuppressWarnings("rawtypes")
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

    @SuppressWarnings("unchecked")
    @Override
    public final Constraint asConstraintDescribed(String constraintName, String constraintDescription) {
        return constraintConstructor.apply(null, constraintName, constraintDescription, constraintWeight, impactType,
                getJustificationMapping(), getIndictedObjectsMapping());
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Constraint asConstraint(String constraintPackage, String constraintName) {
        return constraintConstructor.apply(constraintPackage, constraintName, "", constraintWeight,
                impactType, getJustificationMapping(), getIndictedObjectsMapping());
    }

}
