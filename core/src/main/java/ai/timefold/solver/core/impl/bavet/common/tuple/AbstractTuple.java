package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

/**
 * A tuple is an <i>out tuple</i> in exactly one node and an <i>in tuple</i> in one or more nodes.
 *
 * <p>
 * A tuple must not implement equals()/hashCode() to fact equality,
 * because some stream operations ({@link UniConstraintStream#map(Function)}, ...)
 * might create 2 different tuple instances to contain the same facts
 * and because a tuple's origin may replace a tuple's fact.
 *
 * <p>
 * A tuple is modifiable.
 * However, only the origin node of a tuple (the node where the tuple is the out tuple) may modify it.
 */
public abstract sealed class AbstractTuple permits UniTuple, BiTuple, TriTuple, QuadTuple {

    private static final Object[] EMPTY_STORE = new Object[0];

    private final Object[] store;
    public TupleState state = TupleState.DEAD; // It's the node's job to mark a new tuple as CREATING.

    protected AbstractTuple(int storeSize) {
        this.store = storeSize == 0 ? EMPTY_STORE : new Object[storeSize];
    }

    public final <Value_> Value_ getStore(int index) {
        return (Value_) store[index];
    }

    public final void setStore(int index, Object value) {
        store[index] = value;
    }

    public final <Value_> Value_ removeStore(int index) {
        Value_ value = getStore(index);
        setStore(index, null);
        return value;
    }

}
