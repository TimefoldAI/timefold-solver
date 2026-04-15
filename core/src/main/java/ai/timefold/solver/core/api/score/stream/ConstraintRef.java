package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;

import org.jspecify.annotations.NullMarked;

/**
 * Represents a unique identifier of a constraint.
 * <p>
 * If you need an instance created, use {@link ConstraintRef#of(String)} and not the record's constructors.
 *
 * @param id The constraint id. It must be unique.
 */
@NullMarked
public record ConstraintRef(String id)
        implements
            Comparable<ConstraintRef> {

    public static ConstraintRef of(String id) {
        return new ConstraintRef(id);
    }

    public ConstraintRef {
        id = AbstractConstraintBuilder.sanitize("id", id);
    }

    @Override
    public int compareTo(ConstraintRef other) {
        return id.compareTo(other.id);
    }

}
