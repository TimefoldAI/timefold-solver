package ai.timefold.solver.core.impl.bavet.common;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public interface BavetStream {

    <Stream_> Stream_ getParent();

    default SortedSet<ConstraintNodeLocation> getLocationSet() {
        return new TreeSet<>(Set.of(ConstraintNodeLocation.unknown()));
    }

}
