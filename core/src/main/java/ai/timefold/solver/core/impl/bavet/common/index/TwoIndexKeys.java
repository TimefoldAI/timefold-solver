package ai.timefold.solver.core.impl.bavet.common.index;

record TwoIndexKeys<A, B>(A propertyA, B propertyB) implements IndexKeys {

    @SuppressWarnings("unchecked")
    @Override
    public <Key_> Key_ get(int id) {
        return (Key_) switch (id) {
            case 0 -> propertyA;
            case 1 -> propertyB;
            default -> throw new IllegalArgumentException("Impossible state: index (%d) > 1"
                    .formatted(id));
        };
    }

}
