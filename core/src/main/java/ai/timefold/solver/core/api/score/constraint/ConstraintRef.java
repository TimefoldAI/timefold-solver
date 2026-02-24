package ai.timefold.solver.core.api.score.constraint;

import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;

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

    public static ConstraintRef of(String constraintName) {
        return new ConstraintRef(constraintName);
    }

    public ConstraintRef {
        constraintName = AbstractConstraintBuilder.sanitize("constraintName", constraintName);
    }

    @Override
    public int compareTo(ConstraintRef other) {
        return constraintName.compareTo(other.constraintName);
    }

}
