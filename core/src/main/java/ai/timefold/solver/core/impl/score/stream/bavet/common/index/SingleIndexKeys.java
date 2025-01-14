package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Objects;

record SingleIndexKeys<A>(A property) implements IndexKeys {

    static final SingleIndexKeys<Void> NULL = new SingleIndexKeys<>(null);

    @Override
    public <Type_> Type_ get(int id) {
        if (id != 0) {
            throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        }
        return (Type_) property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { // Due to the use of SingleIndexKeys.NULL, this is possible and likely.
            return true;
        }
        return o instanceof SingleIndexKeys<?> that && Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(property);
    }
}
