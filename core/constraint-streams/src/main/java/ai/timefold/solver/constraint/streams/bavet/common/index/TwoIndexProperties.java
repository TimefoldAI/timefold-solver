package ai.timefold.solver.constraint.streams.bavet.common.index;

import java.util.Objects;

public final class TwoIndexProperties<A, B> implements IndexProperties {
    private final A propertyA;
    private final B propertyB;

    public TwoIndexProperties(A propertyA, B propertyB) {
        this.propertyA = propertyA;
        this.propertyB = propertyB;
    }

    @Override
    public <Type_> Type_ toKey(int id) {
        return (Type_) switch (id) {
            case 0 -> propertyA;
            case 1 -> propertyB;
            default -> throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        };
    }

    public A propertyA() {
        return propertyA;
    }

    public B propertyB() {
        return propertyB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TwoIndexProperties<?, ?> that = (TwoIndexProperties<?, ?>) o;
        return Objects.equals(propertyA, that.propertyA) && Objects.equals(propertyB, that.propertyB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyA, propertyB);
    }
}
