package ai.timefold.solver.core.impl.bavet.uni;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.common.StaticPropagationQueue;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

import org.jspecify.annotations.NullMarked;

/**
 * Filtering nodes are expensive.
 * Considering that most streams start with a nullity check on genuine planning variables,
 * it makes sense to create a specialized version of the node for this case ({@link ForEachExcludingUnassignedUniNode}),
 * as opposed to forcing an extra filter node on the generic case ({@link ForEachIncludingUnassignedUniNode}).
 *
 * @param <A>
 */
@NullMarked
public abstract sealed class AbstractForEachUniNode<A>
        extends AbstractNode
        permits ForEachExcludingUnassignedUniNode, ForEachExcludingPinnedUniNode, ForEachIncludingUnassignedUniNode {

    private final Class<A> forEachClass;
    private final int outputStoreSize;
    private final StaticPropagationQueue<UniTuple<A>> propagationQueue;
    protected final Map<A, UniTuple<A>> tupleMap = new IdentityHashMap<>(1000);

    protected AbstractForEachUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        this.forEachClass = forEachClass;
        this.outputStoreSize = outputStoreSize;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    public void insert(A a) {
        var tuple = new UniTuple<>(a, outputStoreSize);
        var old = tupleMap.put(a, tuple);
        if (old != null) {
            throw new IllegalStateException("The fact (%s) was already inserted, so it cannot insert again."
                    .formatted(a));
        }
        propagationQueue.insert(tuple);
    }

    public abstract void update(A a);

    protected final void updateExisting(A a, UniTuple<A> tuple) {
        var state = tuple.state;
        if (state.isDirty()) {
            if (state == TupleState.DYING || state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (%s) was retracted, so it cannot update."
                        .formatted(a));
            }
            // CREATING or UPDATING is ignored; it's already in the queue.
        } else {
            propagationQueue.update(tuple);
        }
    }

    public void retract(A a) {
        var tuple = tupleMap.remove(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (%s) was never inserted, so it cannot retract."
                    .formatted(a));
        }
        retractExisting(a, tuple);
    }

    protected void retractExisting(A a, UniTuple<A> tuple) {
        var state = tuple.state;
        if (state.isDirty()) {
            if (state == TupleState.DYING || state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (%s) was already retracted, so it cannot retract."
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

    /**
     * Determines if this node supports the given lifecycle operation.
     * Unsupported nodes will not be called during that lifecycle operation.
     *
     * @param lifecycleOperation the lifecycle operation to check
     * @return {@code true} if the given lifecycle operation is supported; otherwise, {@code false}.
     */
    public abstract boolean supports(LifecycleOperation lifecycleOperation);

    @Override
    public final String toString() {
        return "%s(%s)"
                .formatted(getClass().getSimpleName(), forEachClass.getSimpleName());
    }

    /**
     * Represents the various lifecycle operations that can be performed
     * on tuples within a node in Bavet.
     */
    public enum LifecycleOperation {
        /**
         * Represents the operation of inserting a new tuple into the node.
         * This operation is typically performed when a new fact is added to the working solution
         * and needs to be propagated through the node network.
         */
        INSERT,
        /**
         * Represents the operation of updating an existing tuple within the node.
         * This operation is typically triggered when a fact in the working solution
         * is modified, requiring the corresponding tuple to be updated and its changes
         * propagated through the node network.
         */
        UPDATE,
        /**
         * Represents the operation of retracting or removing an existing tuple from the node.
         * This operation is typically used when a fact is removed from the working solution
         * and its corresponding tuple needs to be removed from the node network.
         */
        RETRACT
    }

    public interface InitializableForEachNode<Solution_> extends AutoCloseable {

        void initialize(Solution_ workingSolution, SupplyManager supplyManager);

        @Override
        void close(); // Drop the checked exception.

    }

}
