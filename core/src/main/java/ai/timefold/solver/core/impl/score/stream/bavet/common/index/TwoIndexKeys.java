package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Objects;

record TwoIndexKeys<A, B>(A keyA, B keyB) implements IndexKeys {

    @SuppressWarnings("unchecked")
    @Override
    public <Key_> Key_ get(int id) {
        return (Key_) switch (id) {
            case 0 -> keyA;
            case 1 -> keyB;
            default -> throw new IllegalArgumentException("Impossible state: index (%d) > 1"
                    .formatted(id));
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TwoIndexKeys<?, ?> that &&
                Objects.equals(keyA, that.keyA)
                && Objects.equals(keyB, that.keyB);
    }

    @Override
    public int hashCode() {
        var hash = 1;
        hash = 31 * hash + Objects.hashCode(keyA);
        hash = 31 * hash + Objects.hashCode(keyB);
        return hash;
    }

}
