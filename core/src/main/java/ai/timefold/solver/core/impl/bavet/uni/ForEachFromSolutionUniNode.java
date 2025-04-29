package ai.timefold.solver.core.impl.bavet.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.move.streams.FromSolutionValueCollectingFunction;

import org.jspecify.annotations.NullMarked;

/**
 * Node that reads a property from a planning solution.
 * Since anything directly on a solution is only allowed to change with a new working solution,
 * this node has the following properties:
 * 
 * <ul>
 * <li>Requires initialization when setting new working solution.
 * Inserts at any other time are not allowed.</li>
 * <li>Does not allow retracts. Items can not be removed.</li>
 * <li>Updates should still be possible, since the values may be planning entities.</li>
 * </ul>
 * 
 * @param <Solution_>
 * @param <A>
 */
@NullMarked
public final class ForEachFromSolutionUniNode<Solution_, A>
        extends ForEachIncludingUnassignedUniNode<A>
        implements AbstractForEachUniNode.InitializableForEachNode<Solution_> {

    private final FromSolutionValueCollectingFunction<Solution_, A> valueCollectingFunction;

    private boolean isInitialized = false;

    public ForEachFromSolutionUniNode(FromSolutionValueCollectingFunction<Solution_, A> valueCollectingFunction,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(valueCollectingFunction.declaredClass(), nextNodesTupleLifecycle, outputStoreSize);
        this.valueCollectingFunction = valueCollectingFunction;
    }

    @Override
    public void initialize(Solution_ workingSolution, SupplyManager supplyManager) {
        if (this.isInitialized) { // Failsafe.
            throw new IllegalStateException("Impossible state: initialize() has already been called on %s."
                    .formatted(this));
        } else {
            this.isInitialized = true;
            var valueRange = valueCollectingFunction.apply(workingSolution);
            var valueIterator = valueRange.createOriginalIterator();
            while (valueIterator.hasNext()) {
                var value = valueIterator.next();
                super.insert(value);
            }
        }
    }

    @Override
    public void insert(A a) {
        throw new UnsupportedOperationException("Impossible state: direct insert is not supported on %s."
                .formatted(this));
    }

    @Override
    public void retract(A a) {
        throw new UnsupportedOperationException("Impossible state: direct retract is not supported on %s."
                .formatted(this));
    }

    @Override
    public boolean supports(LifecycleOperation lifecycleOperation) {
        return lifecycleOperation == LifecycleOperation.UPDATE;
    }

    @Override
    public void close() {
        // No need to do anything; initialization doesn't perform anything that'd need cleanup.
    }

}
