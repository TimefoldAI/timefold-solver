package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Arrays;

record ManyIndexKeys(Object... properties) implements IndexKeys {

    static final ManyIndexKeys EMPTY = new ManyIndexKeys();

    @Override
    public <Type_> Type_ get(int id) {
        return (Type_) properties[id];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { // Due to the use of ManyIndexKeys.EMPTY, this is possible.
            return true;
        }
        return o instanceof ManyIndexKeys other && Arrays.equals(properties, other.properties);
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
