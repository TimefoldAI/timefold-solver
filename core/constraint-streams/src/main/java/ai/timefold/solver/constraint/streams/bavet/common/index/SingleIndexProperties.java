package ai.timefold.solver.constraint.streams.bavet.common.index;

record SingleIndexProperties(Object property) implements IndexProperties {

    @Override
    public <Type_> Type_ toKey(int index) {
        if (index != 0) {
            throw new IllegalArgumentException("Impossible state: index (" + index + ") != 0");
        }
        return (Type_) property;
    }

    @Override
    public <Type_> Type_ toKey(int from, int to) {
        if (to != 1) {
            throw new IllegalArgumentException("Impossible state: key from (" + from + ") to (" + to + ").");
        }
        return toKey(from);
    }

}
