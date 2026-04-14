package ai.timefold.solver.core.api.score.stream;

import org.jspecify.annotations.NullMarked;

/**
 * Identifies a {@link Constraint} and optionally carries metadata about it.
 * Users implement this interface, adding any fields they require.
 * <p>
 * <strong>Immutability contract:</strong> {@link #id()} must return the same value
 * for the lifetime of the object.
 * The first value returned is snapshotted
 * when the constraint is built via {@link ConstraintBuilder#asConstraint(ConstraintMetadata)};
 * any later change to the return value of {@link #id()} will be silently ignored
 * and will NOT affect the constraint's identity.
 */
@NullMarked
public interface ConstraintMetadata {

    /**
     * Returns the unique identifier of the constraint.
     * Must be non-null, non-empty, and stable (see class Javadoc).
     *
     * @return the constraint's unique identifier
     */
    String id();

}
