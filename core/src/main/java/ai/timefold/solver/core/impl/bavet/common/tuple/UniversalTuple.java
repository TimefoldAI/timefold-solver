package ai.timefold.solver.core.impl.bavet.common.tuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This is a monomorphic implementation for all tuple cardinalities, helping with performance.
 * Accessed through interfaces (such as {@link UniTuple}) to hide getters/setters from higher cardinalities.
 */
@NullMarked
final class UniversalTuple<A, B, C, D>
        implements UniTuple<A>, BiTuple<A, B>, TriTuple<A, B, C>, QuadTuple<A, B, C, D> {

    private static final Object[] EMPTY_STORE = new Object[0];

    private final int cardinality;
    private @Nullable Object store0;
    private final @Nullable Object[] storeOverflow;

    private @Nullable A a;
    private @Nullable B b;
    private @Nullable C c;
    private @Nullable D d;
    private TupleState state = TupleState.DEAD; // It's the node's job to mark a new tuple as CREATING.

    UniversalTuple(int storeSize, int cardinality) {
        this.cardinality = cardinality;
        this.storeOverflow = storeSize > 1 ? new Object[storeSize - 1] : EMPTY_STORE;
    }

    @Override
    public void setA(@Nullable A a) {
        this.a = a;
    }

    @Override
    public @Nullable A getA() {
        return a;
    }

    @Override
    public void setB(@Nullable B b) {
        this.b = b;
    }

    @Override
    public @Nullable B getB() {
        return b;
    }

    @Override
    public void setC(@Nullable C c) {
        this.c = c;
    }

    @Override
    public @Nullable C getC() {
        return c;
    }

    @Override
    public void setD(@Nullable D d) {
        this.d = d;
    }

    @Override
    public @Nullable D getD() {
        return d;
    }

    @Override
    public TupleState getState() {
        return state;
    }

    @Override
    public void setState(TupleState state) {
        this.state = state;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Value_> @Nullable Value_ getStore(int index) {
        return (Value_) (index == 0 ? store0 : storeOverflow[index - 1]);
    }

    @Override
    public void setStore(int index, @Nullable Object value) {
        if (index == 0) {
            store0 = value;
        } else {
            storeOverflow[index - 1] = value;
        }
    }

    @Override
    public <Value_> @Nullable Value_ removeStore(int index) {
        if (index == 0) {
            var result = (Value_) store0;
            store0 = null;
            return result;
        } else {
            var pos = index - 1;
            var result = (Value_) storeOverflow[pos];
            storeOverflow[pos] = null;
            return result;
        }
    }

    @Override
    public String toString() {
        return switch (cardinality) {
            case 1 -> "{%s}".formatted(getA());
            case 2 -> "{%s, %s}".formatted(getA(), getB());
            case 3 -> "{%s, %s, %s}".formatted(getA(), getB(), getC());
            case 4 -> "{%s, %s, %s, %s}".formatted(getA(), getB(), getC(), getD());
            default -> throw new IllegalStateException("Impossible cardinality: %d.".formatted(cardinality));
        };
    }

}
