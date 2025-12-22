package ai.timefold.solver.core.impl.bavet.common.tuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface BiTuple<A, B> extends Tuple permits UniversalTuple {

    static <A, B> BiTuple<A, B> of(int storeSize) {
        return new UniversalTuple<A, B, Void, Void>(storeSize, 2);
    }

    static <A, B> BiTuple<A, B> of(@Nullable A a, int storeSize) {
        var tuple = BiTuple.<A, B> of(storeSize);
        tuple.setA(a);
        return tuple;
    }

    static <A, B> BiTuple<A, B> of(@Nullable A a, @Nullable B b, int storeSize) {
        var tuple = BiTuple.<A, B> of(a, storeSize);
        tuple.setB(b);
        return tuple;
    }

    @Nullable
    A getA();

    void setA(@Nullable A a);

    @Nullable
    B getB();

    void setB(@Nullable B b);

}
