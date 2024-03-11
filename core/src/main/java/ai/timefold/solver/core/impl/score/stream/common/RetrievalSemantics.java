package ai.timefold.solver.core.impl.score.stream.common;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;

/**
 * Determines the behavior of joins and conditional propagation
 * based on whether they are coming off of a constraint stream started by
 * either {@link ConstraintFactory#from(Class)}
 * or {@link ConstraintFactory#forEach(Class)}
 * family of methods.
 *
 * <p>
 * For classes which are not planning entities, all of their instances are always retrieved.
 * For classes which are planning entities,
 * the difference in behavior depends on whether they use planning variables which allow unassigned values.
 * 
 * @see PlanningVariable#allowsUnassigned()
 */
public enum RetrievalSemantics {

    /**
     * Joins do not include entities with null planning variables,
     * unless specifically requested by join(forEachIncludingUnassigned(...)).
     * Conditional propagation does not include null planning variables,
     * unless specifically requested using a *IncludingUnassigned() method overload.
     *
     * <p>
     * Applies when the stream comes off of a {@link ConstraintFactory#forEach(Class)} family of methods.
     */
    STANDARD,
    /**
     * Joins include entities with null planning variables if these variables allow unassigned values.
     * Conditional propagation always includes entities with null planning variables,
     * regardless of whether their planning variables allow unassigned values.
     *
     * <p>
     * Applies when the stream comes off of a {@link ConstraintFactory#from(Class)}
     * family of methods.
     *
     * @deprecated this semantics is deprecated and kept around for backward compatibility reasons.
     *             It will be removed in 2.0, together with the from() family of methods, along with this entire enum.
     */
    @Deprecated(forRemoval = true)
    LEGACY
}
