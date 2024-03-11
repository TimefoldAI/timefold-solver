package ai.timefold.solver.core.impl.score.stream.common.quad;

import java.util.Collection;
import java.util.Objects;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

public final class QuadConstraintBuilderImpl<A, B, C, D, Score_ extends Score<Score_>>
        extends AbstractConstraintBuilder<Score_>
        implements QuadConstraintBuilder<A, B, C, D, Score_> {

    private PentaFunction<A, B, C, D, Score_, ConstraintJustification> justificationMapping;
    private QuadFunction<A, B, C, D, Collection<Object>> indictedObjectsMapping;

    public QuadConstraintBuilderImpl(QuadConstraintConstructor<A, B, C, D, Score_> constraintConstructor,
            ScoreImpactType impactType, Score_ constraintWeight) {
        super(constraintConstructor, impactType, constraintWeight);
    }

    @Override
    protected PentaFunction<A, B, C, D, Score_, ConstraintJustification> getJustificationMapping() {
        return justificationMapping;
    }

    @Override
    public <ConstraintJustification_ extends ConstraintJustification> QuadConstraintBuilder<A, B, C, D, Score_> justifyWith(
            PentaFunction<A, B, C, D, Score_, ConstraintJustification_> justificationMapping) {
        if (this.justificationMapping != null) {
            throw new IllegalStateException("""
                    Justification mapping already set (%s).
                    Maybe the constraint calls justifyWith() twice?"""
                    .formatted(justificationMapping));
        }
        this.justificationMapping =
                (PentaFunction<A, B, C, D, Score_, ConstraintJustification>) Objects.requireNonNull(justificationMapping);
        return this;
    }

    @Override
    protected QuadFunction<A, B, C, D, Collection<Object>> getIndictedObjectsMapping() {
        return indictedObjectsMapping;
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, Score_>
            indictWith(QuadFunction<A, B, C, D, Collection<Object>> indictedObjectsMapping) {
        if (this.indictedObjectsMapping != null) {
            throw new IllegalStateException("""
                    Indicted objects' mapping already set (%s).
                    Maybe the constraint calls indictWith() twice?"""
                    .formatted(indictedObjectsMapping));
        }
        this.indictedObjectsMapping = Objects.requireNonNull(indictedObjectsMapping);
        return this;
    }

}
