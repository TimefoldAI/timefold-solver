package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;

public interface ConstraintBuilder {

    /**
     * Builds a {@link Constraint} from the constraint stream.
     * The {@link Constraint#getConstraintPackage()} defaults to the package of the {@link PlanningSolution} class.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @return never null
     */
    Constraint asConstraint(String constraintName);

    /**
     * Builds a {@link Constraint} from the constraint stream.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintPackage never null
     * @return never null
     */
    Constraint asConstraint(String constraintPackage, String constraintName);

}
