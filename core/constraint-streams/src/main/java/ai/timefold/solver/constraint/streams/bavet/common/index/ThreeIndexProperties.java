package ai.timefold.solver.constraint.streams.bavet.common.index;

import java.util.Objects;

final class ThreeIndexProperties<A, B, C> implements IndexProperties {

    private final A propertyA;
    private final B propertyB;
    private final C propertyC;

    public ThreeIndexProperties(A propertyA, B propertyB, C propertyC) {
        this.propertyA = propertyA;
        this.propertyB = propertyB;
        this.propertyC = propertyC;
    }

    @Override
    public <Type_> Type_ toKey(int id) {
        return (Type_) switch (id) {
            case 0 -> propertyA;
            case 1 -> propertyB;
            case 2 -> propertyC;
            default -> throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        };
    }

    public A propertyA() {
        return propertyA;
    }

    public B propertyB() {
        return propertyB;
    }

    public C propertyC() {
        return propertyC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ThreeIndexProperties<?, ?, ?> that = (ThreeIndexProperties<?, ?, ?>) o;
        return Objects.equals(propertyA, that.propertyA) && Objects.equals(propertyB,
                that.propertyB) && Objects.equals(propertyC, that.propertyC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyA, propertyB, propertyC);
    }
}
