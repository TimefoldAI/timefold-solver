package ai.timefold.solver.core.impl.bavet.common;

import java.util.Set;

public record ConstraintNodeProfileId(long key, Set<ConstraintNodeLocation> locationSet) {
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
}
