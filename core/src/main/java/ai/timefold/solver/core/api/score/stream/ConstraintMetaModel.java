package ai.timefold.solver.core.api.score.stream;

import java.util.Collection;
import java.util.Set;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

/**
 * Provides information about the known constraints.
 * Works in combination with {@link ConstraintProvider}.
 */
public interface ConstraintMetaModel {

    /**
     * Returns the constraint for the given reference.
     *
     * @param constraintRef never null
     * @return null if such constraint does not exist
     */
    Constraint getConstraint(ConstraintRef constraintRef);

    /**
     * Returns all constraints defined in the {@link ConstraintProvider}.
     *
     * @return never null, iteration order is undefined
     */
    Collection<Constraint> getConstraints();

    /**
     * Returns all constraints from {@link #getConstraints()} that belong to the given group.
     *
     * @param constraintGroup never null
     * @return never null, iteration order is undefined
     */
    Collection<Constraint> getConstraintsPerGroup(String constraintGroup);

    /**
     * Returns constraint groups with at least one constraint in it.
     *
     * @return never null, iteration order is undefined
     */
    Set<String> getConstraintGroups();

}
