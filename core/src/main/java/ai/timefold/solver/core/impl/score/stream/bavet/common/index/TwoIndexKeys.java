package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

record TwoIndexKeys<A, B>(A propertyA, B propertyB) implements IndexKeys {

    @Override
    public <Type_> Type_ get(int id) {
        return (Type_) switch (id) {
            case 0 -> propertyA;
            case 1 -> propertyB;
            default -> throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        };
    }

}
