package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Arrays;

record ManyIndexKeys(Object... properties) implements IndexKeys {

    static final ManyIndexKeys EMPTY = new ManyIndexKeys();
    static final ManyIndexKeys SINGLE_NULL = new ManyIndexKeys((Object) null);

    @SuppressWarnings("unchecked")
    @Override
    public <Key_> Key_ get(int id) {
        return (Key_) properties[id];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { // Due to the use of SINGLE_NULL, this is possible.
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
