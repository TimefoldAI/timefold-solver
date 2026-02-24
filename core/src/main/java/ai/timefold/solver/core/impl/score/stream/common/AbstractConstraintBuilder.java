package ai.timefold.solver.core.impl.score.stream.common;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("rawtypes")
@NullMarked
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

    protected abstract <JustificationMapping_> @Nullable JustificationMapping_ getJustificationMapping();

    protected abstract <IndictedObjectsMapping_> @Nullable IndictedObjectsMapping_ getIndictedObjectsMapping();

    @SuppressWarnings("unchecked")
    @Override
    public final Constraint asConstraintDescribed(String constraintName, String constraintDescription, String constraintGroup) {
        return constraintConstructor.apply(sanitize("constraintName", constraintName), constraintDescription,
                sanitize("constraintGroup", constraintGroup), constraintWeight, impactType, getJustificationMapping(),
                getIndictedObjectsMapping());
    }

    public static String sanitize(String fieldName, String fieldValue) {
        if (fieldValue == null || fieldValue.equalsIgnoreCase("null") || fieldValue.equalsIgnoreCase("nil")) {
            throw new IllegalArgumentException("The %s (%s) cannot be null.".formatted(fieldName, fieldValue));
        }
        if (!fieldValue.matches("^[a-zA-Z0-9]+[a-zA-Z0-9 _.'-]*$")) {
            throw new IllegalArgumentException(
                    """
                            The %s (%s) must only contain alphanumeric characters, spaces, underscores, hyphens, apostrophes ("'") or full stops (".").
                            It must start with an alphanumeric character.
                            Names "null" and "nil" are not allowed either.
                            """
                            .formatted(fieldName, fieldValue));
        }
        var trimmed = fieldValue.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(
                    "The %s (%s) must not be empty or only contain whitespace.".formatted(fieldName, fieldName));
        }
        return trimmed;
    }

}
