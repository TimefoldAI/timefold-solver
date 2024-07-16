package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

public interface ConstraintBuilder {

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * The {@link ConstraintRef#packageName() constraint package} defaults to the package of the {@link PlanningSolution} class.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @return never null
     */
    default Constraint asConstraint(String constraintName) {
        return asConstraintDescribed(constraintName, "");
    }

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * The {@link ConstraintRef#packageName() constraint package} defaults to the package of the {@link PlanningSolution} class.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintDescription never null
     * @return never null
     */
    Constraint asConstraintDescribed(String constraintName, String constraintDescription);

    /**
     * Builds a {@link Constraint} from the constraint stream.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintPackage never null
     * @return never null
     * @deprecated Constraint package should no longer be used, use {@link #asConstraint(String)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    Constraint asConstraint(String constraintPackage, String constraintName);

}
