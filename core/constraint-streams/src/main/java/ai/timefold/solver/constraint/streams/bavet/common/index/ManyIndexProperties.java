package ai.timefold.solver.constraint.streams.bavet.common.index;

import java.util.Arrays;

final class ManyIndexProperties implements IndexProperties {
    private final Object[] properties;

    public ManyIndexProperties(Object... properties) {
        this.properties = properties;
    }

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
