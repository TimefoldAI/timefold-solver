package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

record TwoIndexProperties<A, B>(A propertyA, B propertyB) implements IndexProperties {

    @Override
    public <Type_> Type_ toKey(int id) {
        return (Type_) switch (id) {
            case 0 -> propertyA;
            case 1 -> propertyB;
            default -> throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        };
    }

}
