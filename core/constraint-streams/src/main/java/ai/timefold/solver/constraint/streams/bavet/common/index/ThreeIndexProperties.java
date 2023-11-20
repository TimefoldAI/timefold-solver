package ai.timefold.solver.constraint.streams.bavet.common.index;

import ai.timefold.solver.core.impl.util.Pair;

record ThreeIndexProperties(Object propertyA, Object propertyB, Object propertyC) implements IndexProperties {

    @Override
    public <Type_> Type_ toKey(int index) {
        return switch (index) {
            case 0 -> (Type_) propertyA;
            case 1 -> (Type_) propertyB;
            case 2 -> (Type_) propertyC;
            default -> throw new IllegalArgumentException("Impossible state: index (" + index + ") != 0");
        };
    }

    @Override
    public <Type_> Type_ toKey(int from, int to) {
        return switch (to - from) {
            case 1 -> toKey(from);
            case 2 -> (Type_) new Pair<>(toKey(from), toKey(from + 1));
            case 3 -> {
                if (from != 0 || to != 3) {
                    throw new IllegalArgumentException("Impossible state: key from (" + from + ") to (" + to + ").");
                }
                yield (Type_) this;
            }
            default -> throw new IllegalArgumentException("Impossible state: key from (" + from + ") to (" + to + ").");
        };
    }

}
