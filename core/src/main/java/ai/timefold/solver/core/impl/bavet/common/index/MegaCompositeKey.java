package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Arrays;

record MegaCompositeKey(Object... properties) implements CompositeKey {

    static final MegaCompositeKey EMPTY = new MegaCompositeKey();
    static final MegaCompositeKey SINGLE_NULL = new MegaCompositeKey((Object) null);

    @SuppressWarnings("unchecked")
    @Override
    public <Key_> Key_ get(int id) {
        return (Key_) properties[id];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof MegaCompositeKey that &&
                Arrays.deepEquals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        // Often used in hash-based collections; the JDK-generated default is too slow.
        return Arrays.deepHashCode(properties);
    }

    @Override
    public String toString() {
        return Arrays.toString(properties);
    }

}
