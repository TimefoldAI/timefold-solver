package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.score.director.SessionContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;

    private boolean isInitialized = false;

    @SuppressWarnings("unchecked")
    public ForEachFromSolutionUniNode(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super((Class<A>) valueRangeDescriptor.getVariableDescriptor().getVariablePropertyType(), nextNodesTupleLifecycle,
                outputStoreSize);
        this.valueRangeDescriptor = Objects.requireNonNull(valueRangeDescriptor);
    }

    @Override
    public void initialize(SessionContext<Solution_> context) {
        if (this.isInitialized) { // Failsafe.
            throw new IllegalStateException("Impossible state: initialize() has already been called on %s."
                    .formatted(this));
        } else {
            this.isInitialized = true;
            var valueRangeManager = context.valueRangeManager();
            var valueRange = valueRangeManager.<A> getFromSolution(valueRangeDescriptor, context.workingSolution());
            var valueIterator = valueRange.createOriginalIterator();
            while (valueIterator.hasNext()) {
                var value = valueIterator.next();
                super.insert(value);
            }
        }
    }

    @Override
    public void insert(@Nullable A a) {
        throw new UnsupportedOperationException("Impossible state: direct insert is not supported on %s."
                .formatted(this));
    }

    @Override
    public void retract(@Nullable A a) {
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
