package ai.timefold.solver.core.impl.bavet.uni;

import java.util.HashMap;
import java.util.Map;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.common.StaticPropagationQueue;
import ai.timefold.solver.core.impl.bavet.common.StreamKind;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Filtering nodes are expensive.
 * Considering that most streams start with a nullity check on genuine planning variables,
 * it makes sense to create a specialized version of the node for this case ({@link ForEachFilteredUniNode}),
 * as opposed to forcing an extra filter node on the generic case ({@link ForEachUnfilteredUniNode}).
 *
 * @param <A>
 */
@NullMarked
public abstract sealed class AbstractForEachUniNode<A>
        extends AbstractRootNode<A>
        permits ForEachFilteredUniNode, ForEachUnfilteredUniNode {

    private final Class<A> forEachClass;
    private final int outputStoreSize;
    private final StaticPropagationQueue<UniTuple<A>> propagationQueue;
    protected final Map<A, UniTuple<A>> tupleMap = HashMap.newHashMap(1000);

    protected AbstractForEachUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        this.forEachClass = forEachClass;
        this.outputStoreSize = outputStoreSize;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    @Override
    public StreamKind getStreamKind() {
        return StreamKind.FOR_EACH;
    }

    @Override
    public boolean allowsInstancesOf(Class<?> clazz) {
        return forEachClass.isAssignableFrom(clazz);
    }

    @Override
    public Class<?>[] getSourceClasses() {
        return new Class[] { forEachClass };
    }

    @Override
    public void insert(@Nullable A a) {
        var tuple = UniTuple.of(a, outputStoreSize);
        var old = tupleMap.put(a, tuple);
        if (old != null) {
            throw new IllegalStateException("""
                    The fact (%s) was already inserted.
                    Maybe your entities mistakenly equal each other?
                    Maybe your @%s's collection properties contain duplicates?"""
                    .formatted(a, PlanningSolution.class.getSimpleName()));
        }
        propagationQueue.insert(tuple);
    }

    protected final void updateExisting(@Nullable A a, UniTuple<A> tuple) {
        var state = tuple.getState();
        if (state.isDirty()) {
            if (state == TupleState.DYING || state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (%s) was already retracted."
                        .formatted(a));
            }
            // CREATING or UPDATING is ignored; it's already in the queue.
        } else {
            propagationQueue.update(tuple);
        }
    }

    @Override
    public void retract(@Nullable A a) {
        var tuple = tupleMap.remove(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (%s) was never inserted."
                    .formatted(a));
        }
        retractExisting(a, tuple);
    }

    protected void retractExisting(@Nullable A a, UniTuple<A> tuple) {
        var state = tuple.getState();
        if (state.isDirty()) {
            if (state == TupleState.DYING || state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (%s) was already retracted."
                        .formatted(a));
            }
            propagationQueue.retract(tuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
        } else {
            propagationQueue.retract(tuple, TupleState.DYING);
        }
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

    public final Class<A> getForEachClass() {
        return forEachClass;
    }

    @Override
    public final String toString() {
        return "%s(%s)"
                .formatted(getClass().getSimpleName(), forEachClass.getSimpleName());
    }

}
