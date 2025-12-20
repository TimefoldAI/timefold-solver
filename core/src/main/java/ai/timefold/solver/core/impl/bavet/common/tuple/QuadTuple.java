package ai.timefold.solver.core.impl.bavet.common.tuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface QuadTuple<A, B, C, D> extends Tuple permits UniversalTuple {

    static <A, B, C, D> QuadTuple<A, B, C, D> of(int storeSize) {
        return new UniversalTuple<>(storeSize, 4);
    }

    static <A, B, C, D> QuadTuple<A, B, C, D> of(@Nullable A a, int storeSize) {
        var tuple = QuadTuple.<A, B, C, D> of(storeSize);
        tuple.setA(a);
        return tuple;
    }

    static <A, B, C, D> QuadTuple<A, B, C, D> of(@Nullable A a, @Nullable B b, int storeSize) {
        var tuple = QuadTuple.<A, B, C, D> of(a, storeSize);
        tuple.setB(b);
        return tuple;
    }

    static <A, B, C, D> QuadTuple<A, B, C, D> of(@Nullable A a, @Nullable B b, @Nullable C c, int storeSize) {
        var tuple = QuadTuple.<A, B, C, D> of(a, b, storeSize);
        tuple.setC(c);
        return tuple;
    }

    static <A, B, C, D> QuadTuple<A, B, C, D> of(@Nullable A a, @Nullable B b, @Nullable C c, @Nullable D d, int storeSize) {
        var tuple = QuadTuple.<A, B, C, D> of(a, b, c, storeSize);
        tuple.setD(d);
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

    @Nullable
    D getD();

    void setD(@Nullable D d);

}
