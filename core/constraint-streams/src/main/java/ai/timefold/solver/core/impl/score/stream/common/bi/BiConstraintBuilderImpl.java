package ai.timefold.solver.core.impl.score.stream.common.bi;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

public final class BiConstraintBuilderImpl<A, B, Score_ extends Score<Score_>>
        extends AbstractConstraintBuilder<Score_>
        implements BiConstraintBuilder<A, B, Score_> {

    private TriFunction<A, B, Score_, ConstraintJustification> justificationMapping;
    private BiFunction<A, B, Collection<Object>> indictedObjectsMapping;

    public BiConstraintBuilderImpl(BiConstraintConstructor<A, B, Score_> constraintConstructor, ScoreImpactType impactType,
            Score_ constraintWeight) {
        super(constraintConstructor, impactType, constraintWeight);
    }

    @Override
    protected TriFunction<A, B, Score_, ConstraintJustification> getJustificationMapping() {
        return justificationMapping;
    }

    @Override
    public <ConstraintJustification_ extends ConstraintJustification> BiConstraintBuilder<A, B, Score_> justifyWith(
            TriFunction<A, B, Score_, ConstraintJustification_> justificationMapping) {
        if (this.justificationMapping != null) {
            throw new IllegalStateException("""
                    Justification mapping already set (%s).
                    Maybe the constraint calls justifyWith() twice?"""
                    .formatted(justificationMapping));
        }
        this.justificationMapping =
                (TriFunction<A, B, Score_, ConstraintJustification>) Objects.requireNonNull(justificationMapping);
        return this;
    }

    @Override
    protected BiFunction<A, B, Collection<Object>> getIndictedObjectsMapping() {
        return indictedObjectsMapping;
    }

    @Override
    public BiConstraintBuilder<A, B, Score_> indictWith(BiFunction<A, B, Collection<Object>> indictedObjectsMapping) {
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
