package ai.timefold.solver.core.impl.bavet.common.tuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface TriTuple<A, B, C> extends Tuple permits UniversalTuple {

    static <A, B, C> TriTuple<A, B, C> of(int storeSize) {
        return new UniversalTuple<A, B, C, Void>(storeSize, 3);
    }

    static <A, B, C> TriTuple<A, B, C> of(@Nullable A a, int storeSize) {
        var tuple = TriTuple.<A, B, C> of(storeSize);
        tuple.setA(a);
        return tuple;
    }

    static <A, B, C> TriTuple<A, B, C> of(@Nullable A a, @Nullable B b, int storeSize) {
        var tuple = TriTuple.<A, B, C> of(a, storeSize);
        tuple.setB(b);
        return tuple;
    }

    static <A, B, C> TriTuple<A, B, C> of(@Nullable A a, @Nullable B b, @Nullable C c, int storeSize) {
        var tuple = TriTuple.<A, B, C> of(a, b, storeSize);
        tuple.setC(c);
        return tuple;
    }

    @Nullable
    A getA();

    void setA(@Nullable A a);

    @Nullable
    B getB();

    void setB(@Nullable B b);

    @Nullable
    C getC();

    void setC(@Nullable C c);

}
