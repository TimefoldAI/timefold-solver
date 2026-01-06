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

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        return o instanceof BiCompositeKey<?, ?> that &&
                ((propertyA == that.propertyA) || (propertyA != null && propertyA.equals(that.propertyA))) &&
                ((propertyB == that.propertyB) || (propertyB != null && propertyB.equals(that.propertyB)));
    }

    @Override
    public int hashCode() {
        // Often used in hash-based collections; the JDK-generated default is too slow.
        // We do not use Objects.hash(...) because it creates an array each time.
        // We do not use Objects.hashCode() due to https://bugs.openjdk.org/browse/JDK-8015417.
        var hash = 1;
        hash = 31 * hash + (propertyA == null ? 0 : propertyA.hashCode());
        return 31 * hash + (propertyB == null ? 0 : propertyB.hashCode());
    }

}
