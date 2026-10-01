package ai.timefold.solver.core.impl.score.stream.common.uni;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class UniConstraintBuilderImpl<A, Score_ extends Score<Score_>> extends AbstractConstraintBuilder<Score_>
        implements UniConstraintBuilder<A, Score_> {

    private @Nullable BiFunction<A, Score_, ConstraintJustification> justificationMapping;

    public UniConstraintBuilderImpl(UniConstraintConstructor<A, Score_> constraintConstructor, ScoreImpactType impactType,
            Score_ constraintWeight) {
        super(constraintConstructor, impactType, constraintWeight);
    }

    @Override
    protected @Nullable BiFunction<A, Score_, ConstraintJustification> getJustificationMapping() {
        return justificationMapping;
    }

    @Override
    public <ConstraintJustification_ extends ConstraintJustification> UniConstraintBuilder<A, Score_>
            justifyWith(BiFunction<A, Score_, ConstraintJustification_> justificationMapping) {
        if (this.justificationMapping != null) {
            throw new IllegalStateException("""
                    Justification mapping already set (%s).
                    Maybe the constraint calls justifyWith() twice?""".formatted(justificationMapping));
        }
        this.justificationMapping =
                (BiFunction<A, Score_, ConstraintJustification>) Objects.requireNonNull(justificationMapping);
        return this;
    }

}
