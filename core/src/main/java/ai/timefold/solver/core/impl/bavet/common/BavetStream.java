package ai.timefold.solver.core.impl.bavet.common;

import java.util.Set;

public interface BavetStream {

    <Stream_> Stream_ getParent();

    default Set<ConstraintNodeLocation> getLocationSet() {
        return Set.of(ConstraintNodeLocation.unknown());
    }

}
