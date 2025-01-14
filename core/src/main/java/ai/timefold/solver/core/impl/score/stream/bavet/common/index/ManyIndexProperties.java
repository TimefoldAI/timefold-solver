package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Arrays;

record ManyIndexProperties(Object... properties) implements IndexProperties {

    static final ManyIndexProperties EMPTY = new ManyIndexProperties();

    @Override
    public <Type_> Type_ toKey(int id) {
        return (Type_) properties[id];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { // Due to the use of ManyIndexProperties.EMPTY, this is possible.
            return true;
        }
        return o instanceof ManyIndexProperties other && Arrays.equals(properties, other.properties);
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
