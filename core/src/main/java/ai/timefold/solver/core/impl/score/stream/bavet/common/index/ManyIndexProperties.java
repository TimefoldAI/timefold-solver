package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Arrays;

record ManyIndexProperties(Object... properties) implements IndexProperties {

    @Override
    public <Type_> Type_ toKey(int id) {
        return (Type_) properties[id];
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ManyIndexProperties other) {
            return Arrays.equals(properties, other.properties);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(properties);
    }

    @Override
    public String toString() {
        return Arrays.toString(properties);
    }

}
