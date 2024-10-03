package ai.timefold.solver.core.impl.score.stream.common.uni;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

import org.jspecify.annotations.NonNull;

public final class UniConstraintBuilderImpl<A, Score_ extends Score<Score_>>
        extends AbstractConstraintBuilder<Score_>
        implements UniConstraintBuilder<A, Score_> {

    private BiFunction<A, Score_, ConstraintJustification> justificationMapping;
    private Function<A, Collection<Object>> indictedObjectsMapping;

    public UniConstraintBuilderImpl(UniConstraintConstructor<A, Score_> constraintConstructor, ScoreImpactType impactType,
            Score_ constraintWeight) {
        super(constraintConstructor, impactType, constraintWeight);
    }

    @Override
    protected BiFunction<A, Score_, ConstraintJustification> getJustificationMapping() {
        return justificationMapping;
    }

    @Override
    public @NonNull <ConstraintJustification_ extends ConstraintJustification> UniConstraintBuilder<A, Score_> justifyWith(
            @NonNull BiFunction<A, Score_, ConstraintJustification_> justificationMapping) {
        if (this.justificationMapping != null) {
            throw new IllegalStateException("""
                    Justification mapping already set (%s).
                    Maybe the constraint calls justifyWith() twice?"""
                    .formatted(justificationMapping));
        }
        this.justificationMapping =
                (BiFunction<A, Score_, ConstraintJustification>) Objects.requireNonNull(justificationMapping);
        return this;
    }

    @Override
    protected Function<A, Collection<Object>> getIndictedObjectsMapping() {
        return indictedObjectsMapping;
    }

    @Override
    public @NonNull UniConstraintBuilder<A, Score_>
            indictWith(@NonNull Function<A, Collection<Object>> indictedObjectsMapping) {
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
