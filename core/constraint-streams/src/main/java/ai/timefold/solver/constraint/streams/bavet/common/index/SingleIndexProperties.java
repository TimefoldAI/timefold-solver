package ai.timefold.solver.constraint.streams.bavet.common.index;

record SingleIndexProperties<A>(A property) implements IndexProperties {

    @Override
    public <Type_> Type_ toKey(int id) {
        if (id != 0) {
            throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        }
        return (Type_) property;
    }

}
