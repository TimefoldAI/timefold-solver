package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Objects;

record SingleIndexProperties<A>(A property) implements IndexProperties {

    static final SingleIndexProperties<Void> NULL = new SingleIndexProperties<>(null);

    @Override
    public <Type_> Type_ toKey(int id) {
        if (id != 0) {
            throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        }
        return (Type_) property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { // Due to the use of SingleIndexProperties.NULL, this is possible and likely.
            return true;
        }
        return o instanceof SingleIndexProperties<?> that && Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(property);
    }
}
