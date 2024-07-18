package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

/**
 * This represents a single constraint in the {@link ConstraintStream} API
 * that impacts the {@link Score}.
 * It is defined in {@link ConstraintProvider#defineConstraints(ConstraintFactory)}
 * by calling {@link ConstraintFactory#forEach(Class)}.
 */
public interface Constraint {

    /**
     * The {@link ConstraintFactory} that built this.
     *
     * @deprecated for removal as it is not necessary on the public API.
     * @return never null
     */
    @Deprecated(forRemoval = true)
    ConstraintFactory getConstraintFactory();

    ConstraintRef getConstraintRef();

    /**
     * Returns a human-friendly description of the constraint.
     * The format of the description is left unspecified and will not be parsed in any way.
     *
     * @return never null, may be left empty
     */
    default String getDescription() {
        return "";
    }

    /**
     * @deprecated Prefer {@link #getConstraintRef()}.
     * @return never null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    default String getConstraintPackage() {
        return getConstraintRef().packageName();
    }

    /**
     * @deprecated Prefer {@link #getConstraintRef()}.
     * @return never null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    default String getConstraintName() {
        return getConstraintRef().constraintName();
    }

    /**
     * @deprecated Prefer {@link #getConstraintRef()}.
     * @return never null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    default String getConstraintId() {
        return getConstraintRef().constraintId();
    }

}
