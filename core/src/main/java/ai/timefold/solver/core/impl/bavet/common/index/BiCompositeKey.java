package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Objects;

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
    public boolean equals(Object o) { // Often used in hash-based collections; the JDK-generated default is too slow.
        if (this == o) {
            return true;
        }
        return o instanceof BiCompositeKey<?, ?> that &&
                Objects.equals(propertyA, that.propertyA) &&
                Objects.equals(propertyB, that.propertyB);
    }

    @Override
    public int hashCode() {
        var hash = 7;
        hash = 31 * hash + Objects.hashCode(propertyA);
        hash = 31 * hash + Objects.hashCode(propertyB);
        return hash;
    }

    @Override
    public String toString() {
        return "{" + propertyA + ", " + propertyB + "}";
    }

}
