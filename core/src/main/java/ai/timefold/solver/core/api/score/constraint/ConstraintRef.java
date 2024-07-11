package ai.timefold.solver.core.api.score.constraint;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;

/**
 * Represents a unique identifier of a constraint.
 * <p>
 * Users should have no need to create instances of this record.
 * If necessary, use {@link ConstraintRef#of(String, String)} and not the record's constructors.
 *
 * @param packageName The constraint package is the namespace of the constraint.
 *        When using a {@link ConstraintConfiguration},
 *        it is equal to the {@link ConstraintWeight#constraintPackage()}.
 *        It is not recommended for the user to set this, or to read its value;
 *        instead, the user should use whatever the solver provided as default and not rely on this information at all.
 *        The entire concept of constraint package is likely to be removed in a future version of the solver.
 * @param constraintName The constraint name.
 *        It might not be unique, but {@link #constraintId()} is unique.
 *        When using a {@link ConstraintConfiguration},
 *        it is equal to the {@link ConstraintWeight#value()}.
 * @param constraintId Always derived from {@code packageName} and {@code constraintName}.
 */
public record ConstraintRef(String packageName, String constraintName, String constraintId)
        implements
            Comparable<ConstraintRef> {

    private static final char PACKAGE_SEPARATOR = '/';

    public static ConstraintRef of(String packageName, String constraintName) {
        return new ConstraintRef(packageName, constraintName, null);
    }

    public static ConstraintRef parseId(String constraintId) {
        var slashIndex = constraintId.indexOf(PACKAGE_SEPARATOR);
        if (slashIndex < 0) {
            throw new IllegalArgumentException(
                    "The constraintId (%s) is invalid as it does not contain a package separator (%s)."
                            .formatted(constraintId, PACKAGE_SEPARATOR));
        }
        var packageName = constraintId.substring(0, slashIndex);
        var constraintName = constraintId.substring(slashIndex + 1);
        return new ConstraintRef(packageName, constraintName, constraintId);

    }

    public static String composeConstraintId(String packageName, String constraintName) {
        return packageName + PACKAGE_SEPARATOR + constraintName;
    }

    public ConstraintRef {
        packageName = validate(packageName, "constraint package");
        constraintName = validate(constraintName, "constraint name");
        var expectedConstraintId = composeConstraintId(packageName, constraintName);
        if (constraintId != null && !constraintId.equals(expectedConstraintId)) {
            throw new IllegalArgumentException(
                    "Specifying custom constraintId (%s) is not allowed."
                            .formatted(constraintId));
        }
        constraintId = expectedConstraintId;
    }

    private static String validate(String identifier, String type) {
        var sanitized = Objects.requireNonNull(identifier).trim();
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("The %s cannot be empty."
                    .formatted(type));
        } else if (sanitized.contains("" + PACKAGE_SEPARATOR)) {
            throw new IllegalArgumentException("The %s (%s) cannot contain a package separator (%s)."
                    .formatted(type, sanitized, PACKAGE_SEPARATOR));
        }
        return sanitized;
    }

    @Override
    public String toString() {
        return constraintId;
    }

    @Override
    public int compareTo(ConstraintRef other) {
        return constraintId.compareTo(other.constraintId);
    }

}
