package ai.timefold.solver.core.impl.bavet.common;

import java.util.SortedSet;

import org.jspecify.annotations.NullMarked;

/**
 * A unique identifier for a constraint node profile.
 *
 * @param key the unique key of the constraint node in a given network
 * @param networkId the network the constraint node belongs to
 * @param streamKind the kind of stream (e.g., JOIN, FILTER)
 * @param qualifier an optional qualifier to distinguish parts of the same node, such as left and right input
 * @param locationSet the set of locations where this constraint node is defined
 */
@NullMarked
public record ConstraintNodeProfileId(int key, int networkId, StreamKind streamKind, Qualifier qualifier,
        SortedSet<ConstraintNodeLocation> locationSet)
        implements
            Comparable<ConstraintNodeProfileId> {

    public ConstraintNodeProfileId(int key, int networkId, StreamKind streamKind,
            SortedSet<ConstraintNodeLocation> locationSet) {
        this(key, networkId, streamKind, Qualifier.NONE, locationSet);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ConstraintNodeProfileId that))
            return false;
        return key == that.key && networkId == that.networkId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(key | ((long) networkId) << Integer.SIZE);
    }

    @Override
    public int compareTo(ConstraintNodeProfileId other) {
        var keyComparisonResult = Integer.compare(key, other.key);
        if (keyComparisonResult == 0) {
            return Integer.compare(networkId, other.networkId);
        }
        return keyComparisonResult;
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
            var location = locationSet.getFirst();
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
