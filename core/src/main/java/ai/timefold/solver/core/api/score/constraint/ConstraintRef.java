package ai.timefold.solver.core.api.score.constraint;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

/**
 * Represents a unique identifier of a constraint.
 * <p>
 * Users should have no need to create instances of this record.
 * If necessary, use {@link ConstraintRef#of(String)} and not the record's constructors.
 *
 * @param constraintName The constraint name. It must be unique.
 */
@NullMarked
public record ConstraintRef(String constraintName)
        implements
            Comparable<ConstraintRef> {

    private static final char PACKAGE_SEPARATOR = '/';

    public static ConstraintRef of(String constraintName) {
        return new ConstraintRef(constraintName);
    }

    public ConstraintRef {
        var sanitized = Objects.requireNonNull(constraintName).trim();
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("The %s cannot be empty."
                    .formatted("constraint name"));
        } else if (sanitized.contains("" + PACKAGE_SEPARATOR)) {
            throw new IllegalArgumentException("The %s (%s) cannot contain a package separator (%s)."
                    .formatted("constraint name", sanitized, PACKAGE_SEPARATOR));
        }
        constraintName = sanitized;
    }

    @Override
    public int compareTo(ConstraintRef other) {
        return constraintName.compareTo(other.constraintName);
    }

}
