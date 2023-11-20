package ai.timefold.solver.constraint.streams.bavet.common.index;

record TwoIndexProperties(Object propertyA, Object propertyB) implements IndexProperties {

    @Override
    public <Type_> Type_ toKey(int index) {
        return switch (index) {
            case 0 -> (Type_) propertyA;
            case 1 -> (Type_) propertyB;
            default -> throw new IllegalArgumentException("Impossible state: index (" + index + ") != 0");
        };
    }

    @Override
    public <Type_> Type_ toKey(int from, int to) {
        return switch (to - from) {
            case 1 -> toKey(from);
            case 2 -> {
                if (from != 0 || to != 2) {
                    throw new IllegalArgumentException("Impossible state: key from (" + from + ") to (" + to + ").");
                }
                yield (Type_) this;
            }
            default -> throw new IllegalArgumentException("Impossible state: key from (" + from + ") to (" + to + ").");
        };
    }

}
