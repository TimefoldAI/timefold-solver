package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

record ThreeIndexKeys<A, B, C>(A propertyA, B propertyB, C propertyC) implements IndexKeys {

    @Override
    public <Type_> Type_ get(int id) {
        return (Type_) switch (id) {
            case 0 -> propertyA;
            case 1 -> propertyB;
            case 2 -> propertyC;
            default -> throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        };
    }

}
