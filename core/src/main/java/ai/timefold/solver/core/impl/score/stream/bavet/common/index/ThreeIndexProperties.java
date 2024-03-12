package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

record ThreeIndexProperties<A, B, C>(A propertyA, B propertyB, C propertyC) implements IndexProperties {

    @Override
    public <Type_> Type_ toKey(int id) {
        return (Type_) switch (id) {
            case 0 -> propertyA;
            case 1 -> propertyB;
            case 2 -> propertyC;
            default -> throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        };
    }

}
