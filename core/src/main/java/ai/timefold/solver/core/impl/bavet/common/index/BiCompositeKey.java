package ai.timefold.solver.core.impl.bavet.common.index;

record BiCompositeKey<A, B>(A propertyA, B propertyB) implements CompositeKey {

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
