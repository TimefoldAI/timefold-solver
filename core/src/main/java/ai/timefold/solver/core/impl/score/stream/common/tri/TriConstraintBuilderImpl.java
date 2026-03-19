package ai.timefold.solver.core.impl.score.stream.common.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class TriConstraintBuilderImpl<A, B, C, Score_ extends Score<Score_>> extends AbstractConstraintBuilder<Score_>
        implements TriConstraintBuilder<A, B, C, Score_> {

    private @Nullable QuadFunction<A, B, C, Score_, ConstraintJustification> justificationMapping;

    public TriConstraintBuilderImpl(TriConstraintConstructor<A, B, C, Score_> constraintConstructor, ScoreImpactType impactType,
            Score_ constraintWeight) {
        super(constraintConstructor, impactType, constraintWeight);
    }

    @Override
    protected @Nullable QuadFunction<A, B, C, Score_, ConstraintJustification> getJustificationMapping() {
        return justificationMapping;
    }

    @Override
    public <ConstraintJustification_ extends ConstraintJustification> TriConstraintBuilder<A, B, C, Score_>
            justifyWith(QuadFunction<A, B, C, Score_, ConstraintJustification_> justificationMapping) {
        if (this.justificationMapping != null) {
            throw new IllegalStateException("""
                    Justification mapping already set (%s).
                    Maybe the constraint calls justifyWith() twice?""".formatted(justificationMapping));
        }
        this.justificationMapping =
                (QuadFunction<A, B, C, Score_, ConstraintJustification>) Objects.requireNonNull(justificationMapping);
        return this;
    }

}
