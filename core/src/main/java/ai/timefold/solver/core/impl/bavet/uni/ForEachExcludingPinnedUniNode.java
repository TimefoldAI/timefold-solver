package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningEntityMetaModel;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ForEachExcludingPinnedUniNode<Solution_, A>
        extends AbstractForEachUniNode<A>
        implements AbstractForEachUniNode.InitializableForEachNode<Solution_> {

    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_TRUE = entity -> true;

    private final EntityDescriptor<Solution_> entityDescriptor;

    private @Nullable ListVariableStateSupply<Solution_> listVariableStateSupply;
    private @SuppressWarnings("unchecked") Predicate<A> filter = ALWAYS_TRUE;

    /**
     * 
     * @param entityMetaModel Expects a pinnable entity.
     *        Every other option should have already been exluded and passed to a different kind of node.
     * @param nextNodesTupleLifecycle
     * @param outputStoreSize
     */
    public ForEachExcludingPinnedUniNode(PlanningEntityMetaModel<Solution_, A> entityMetaModel,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(Objects.requireNonNull(entityMetaModel).type(), nextNodesTupleLifecycle, outputStoreSize);
        this.entityDescriptor = ((DefaultPlanningEntityMetaModel<Solution_, A>) entityMetaModel).entityDescriptor();
    }

    @Override
    public void initialize(Solution_ workingSolution, SupplyManager supplyManager) {
        this.filter = buildFilter(workingSolution, supplyManager);
    }

    private Predicate<A> buildFilter(Solution_ workingSolution, SupplyManager supplyManager) {
        if (entityDescriptor.isGenuine()) {
            return entity -> entityDescriptor.isMovable(workingSolution, entity);
        }
        // Shadow entities can only be pinned if elements in a genuine entity's list variable.
        var solutionDescriptor = entityDescriptor.getSolutionDescriptor();
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        this.listVariableStateSupply = Objects.requireNonNull(supplyManager.demand(listVariableDescriptor.getStateDemand()));
        return entity -> !listVariableStateSupply.isPinned(entity);
    }

    @Override
    public void insert(A a) {
        if (!filter.test(a)) { // Skip inserting the tuple as it does not pass the filter.
            return;
        }
        super.insert(a);
    }

    @Override
    public void update(A a) {
        var tuple = tupleMap.get(a);
        if (tuple == null) { // The tuple was never inserted because it did not pass the filter.
            insert(a);
        } else if (filter.test(a)) {
            updateExisting(a, tuple);
        } else { // Tuple no longer passes the filter.
            retract(a);
        }
    }

    @Override
    public void retract(A a) {
        var tuple = tupleMap.remove(a);
        if (tuple == null) { // The tuple was never inserted because it did not pass the filter.
            return;
        }
        super.retractExisting(a, tuple);
    }

    @Override
    public boolean supports(LifecycleOperation lifecycleOperation) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void close() {
        filter = ALWAYS_TRUE;
        if (listVariableStateSupply != null) {
            listVariableStateSupply.close();
            listVariableStateSupply = null;
        }
    }

}
