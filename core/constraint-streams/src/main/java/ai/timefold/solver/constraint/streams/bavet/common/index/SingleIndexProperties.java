package ai.timefold.solver.constraint.streams.bavet.common.index;

import java.util.Objects;

final class SingleIndexProperties<A> implements IndexProperties {

    private final A property;

    public SingleIndexProperties(A property) {
        this.property = property;
    }

    public A property() {
        return property;
    }

    @Override
    public <Type_> Type_ toKey(int id) {
        if (id != 0) {
            throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        }
        return (Type_) property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SingleIndexProperties<?> that = (SingleIndexProperties<?>) o;
        return Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property);
    }
}
