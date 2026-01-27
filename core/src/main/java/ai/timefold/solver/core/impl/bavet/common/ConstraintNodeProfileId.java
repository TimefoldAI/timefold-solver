package ai.timefold.solver.core.impl.bavet.common;

import java.util.SortedSet;

import org.jspecify.annotations.NullMarked;

/**
 * A unique identifier for a constraint node profile.
 *
 * @param key the unique key of the constraint node
 * @param streamKind the kind of stream (e.g., JOIN, FILTER)
 * @param qualifier an optional qualifier to distinguish parts of the same node, such as left and right input
 * @param locationSet the set of locations where this constraint node is defined
 */
@NullMarked
public record ConstraintNodeProfileId(long key, StreamKind streamKind, Qualifier qualifier,
        SortedSet<ConstraintNodeLocation> locationSet)
        implements
            Comparable<ConstraintNodeProfileId> {

    public ConstraintNodeProfileId(long key, StreamKind streamKind, SortedSet<ConstraintNodeLocation> locationSet) {
        this(key, streamKind, Qualifier.NONE, locationSet);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ConstraintNodeProfileId that))
            return false;
        return key == that.key;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(key);
    }

    @Override
    public int compareTo(ConstraintNodeProfileId other) {
        return Long.compare(key, other.key);
    }

    @Override
    public String toString() {
        if (qualifier == Qualifier.NONE) {
            return "%s %d".formatted(streamKind, key);
        } else {
            var qualifierString = switch (qualifier) {
                case NODE -> "Node";
                case LEFT_INPUT -> "Left Input";
                case RIGHT_INPUT -> "Right Input";
                default -> throw new IllegalStateException("Impossible state: unknown qualifier (%s)."
                        .formatted(qualifier));
            };
            return "%s %s %d".formatted(streamKind, qualifierString, key);
        }
    }

    /**
     * Unlike {@link #toString()}, includes location information.
     * This is more verbose and intended for explanatory messages.
     *
     * @return a verbose string representation of this profile ID
     */
    public String toVerboseString() {
        var toString = toString();
        if (locationSet.size() == 1) {
            var location = locationSet.iterator().next();
            return "%s defined at location %s".formatted(toString, location);
        }
        return "%s shared at locations %s".formatted(toString, locationSet);
    }

    public enum Qualifier {
        /**
         * Most typically.
         */
        NODE,
        /**
         * For {@link AbstractTwoInputNode}.
         */
        LEFT_INPUT,
        /**
         * For {@link AbstractTwoInputNode}.
         */
        RIGHT_INPUT,
        /**
         * For filters and scoring.
         */
        NONE
    }

}
