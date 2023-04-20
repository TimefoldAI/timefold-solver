package ai.timefold.solver.examples.common.util;

public interface Pair<A, B> {

    static <A, B> Pair<A, B> of(A key, B value) {
        return new PairImpl<>(key, value);
    }

    A getKey();

    B getValue();

}
