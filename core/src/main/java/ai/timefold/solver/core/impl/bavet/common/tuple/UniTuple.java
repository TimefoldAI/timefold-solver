package ai.timefold.solver.core.impl.bavet.common.tuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface UniTuple<A> extends Tuple permits UniversalTuple {

    static <A> UniTuple<A> of(int storeSize) {
        return new UniversalTuple<A, Void, Void, Void>(storeSize, 1);
    }

    static <A> UniTuple<A> of(@Nullable A a, int storeSize) {
        var tuple = UniTuple.<A> of(storeSize);
        tuple.setA(a);
        return tuple;
    }

    @Nullable
    A getA();

    void setA(@Nullable A a);

}
