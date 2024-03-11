package ai.timefold.solver.core.impl.score.stream.bavet.common.tuple;

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

    /*
     * We create a lot of tuples, many of them having store size of 1.
     * If an array of size 1 was created for each such tuple, memory would be wasted and indirection created.
     * This trade-off of increased memory efficiency for marginally slower access time is proven beneficial.
     */
    private final boolean storeIsArray;

    private Object store;
    public TupleState state = TupleState.DEAD; // It's the node's job to mark a new tuple as CREATING.

    protected AbstractTuple(int storeSize) {
        this.store = (storeSize < 2) ? null : new Object[storeSize];
        this.storeIsArray = store != null;
    }

    public final <Value_> Value_ getStore(int index) {
        return (Value_) (storeIsArray ? ((Object[]) store)[index] : store);
    }

    public final void setStore(int index, Object value) {
        if (storeIsArray) {
            ((Object[]) store)[index] = value;
        } else {
            store = value;
        }
    }

    public <Value_> Value_ removeStore(int index) {
        Value_ value;
        if (storeIsArray) {
            Object[] array = (Object[]) store;
            value = (Value_) array[index];
            array[index] = null;
        } else {
            value = (Value_) store;
            store = null;
        }
        return value;
    }

}
