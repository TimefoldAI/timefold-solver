package ai.timefold.solver.core.impl.bavet.common;

import java.util.SortedSet;

public record ConstraintNodeProfileId(long key,
        StreamKind streamKind,
        SortedSet<ConstraintNodeLocation> locationSet,
        boolean isPropagator) implements Comparable<ConstraintNodeProfileId> {

    public ConstraintNodeProfileId(long key, StreamKind streamKind, SortedSet<ConstraintNodeLocation> locationSet) {
        this(key, streamKind, locationSet, false);
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
        var profileKind = isPropagator ? "Propagator" : "Node";
        if (locationSet.size() == 1) {
            var location = locationSet.iterator().next();
            return "%s %s %d defined at location %s".formatted(streamKind, profileKind, key, location);
        }
        return "%s %s %d shared at locations %s".formatted(streamKind, profileKind, key, locationSet);
    }
}
