package ai.timefold.solver.constraint.streams.bavet.common.index;

import java.util.Arrays;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

record ManyIndexProperties(Object... properties) implements IndexProperties {

    @Override
    public <Type_> Type_ toKey(int index) {
        return (Type_) properties[index];
    }

    @Override
    public <Type_> Type_ toKey(int from, int to) {
        return switch (to - from) {
            case 1 -> toKey(from);
            case 2 -> (Type_) new Pair<>(toKey(from), toKey(from + 1));
            case 3 -> (Type_) new Triple<>(toKey(from), toKey(from + 1), toKey(from + 2));
            case 4 -> (Type_) new Quadruple<>(toKey(from), toKey(from + 1), toKey(from + 2), toKey(from + 3));
            default -> (Type_) new IndexerKey(this, from, to);
        };
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
