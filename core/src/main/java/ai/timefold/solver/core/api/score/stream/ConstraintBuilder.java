package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;

import org.jspecify.annotations.NonNull;

public interface ConstraintBuilder {

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * The constraint will be placed in the {@link Constraint#DEFAULT_CONSTRAINT_GROUP default constraint group}.
     *
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     */
    default @NonNull Constraint asConstraint(@NonNull String constraintName) {
        return asConstraintDescribed(constraintName, "");
    }

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * The constraint will be placed in the {@link Constraint#DEFAULT_CONSTRAINT_GROUP default constraint group}.
     *
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     */
    @NonNull
    default Constraint asConstraintDescribed(@NonNull String constraintName, @NonNull String constraintDescription) {
        return asConstraintDescribed(constraintName, constraintDescription, Constraint.DEFAULT_CONSTRAINT_GROUP);
    }

    /**
     * Builds a {@link Constraint} from the constraint stream.
     *
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintGroup only allows alphanumeric characters, "-" and "_"
     */
    @NonNull
    Constraint asConstraintDescribed(@NonNull String constraintName, @NonNull String constraintDescription,
            @NonNull String constraintGroup);

}
