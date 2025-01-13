package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Objects;

record SingleIndexProperties<A>(A property) implements IndexProperties {

    // Used often enough for the singleton to make meaningful memory savings.
    private static final SingleIndexProperties<Void> NULL_INSTANCE = new SingleIndexProperties<>(null);

    @SuppressWarnings("unchecked")
    public static <A> SingleIndexProperties<A> of(A property) {
        return property == null ? (SingleIndexProperties<A>) NULL_INSTANCE : new SingleIndexProperties<>(property);
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
        if (this == o) {
            return true;
        }
        return o instanceof SingleIndexProperties<?> other && Objects.equals(property, other.property);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(property);
    }
}
