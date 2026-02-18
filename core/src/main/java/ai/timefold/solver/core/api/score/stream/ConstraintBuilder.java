package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConstraintBuilder {

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * The constraint will be placed in the {@link Constraint#DEFAULT_CONSTRAINT_GROUP default constraint group}.
     *
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     */
    default Constraint asConstraint(String constraintName) {
        return asConstraintDescribed(constraintName, "");
    }

    /**
     * As defined by {@link #asConstraintDescribed(String, String, String)},
     * placing the constraint in the {@link Constraint#DEFAULT_CONSTRAINT_GROUP default constraint group}.
     *
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     */
    default Constraint asConstraintDescribed(String constraintName, String constraintDescription) {
        return asConstraintDescribed(constraintName, constraintDescription, Constraint.DEFAULT_CONSTRAINT_GROUP);
    }

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * Both the constraint name and the constraint group are only allowed
     * to contain alphanumeric characters, " ", "-" or "_".
     * The constraint description can contain any character, but it is recommended to keep it short and concise.
     * <p>
     * Unlike the constraint name and group,
     * the constraint description is unlikely to be used externally as an identifier,
     * and therefore doesn't need to be URL-friendly, or protected against injection attacks.
     *
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintGroup not used by the solver directly, but may be used by external tools to group constraints together,
     *        such as by their source or by their purpose
     */
    Constraint asConstraintDescribed(String constraintName, String constraintDescription, String constraintGroup);

}
