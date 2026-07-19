package ai.timefold.solver.core.impl.domain.variable.violation;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * Tracks shadow variable update events for a given {@link ai.timefold.solver.core.api.domain.variable.PlanningListVariable}.
 */
@NullMarked
public class ListVariableTracker<Solution_> implements Supply {
    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final List<Object> beforeVariableChangedEntityList;
    private final List<Object> afterVariableChangedEntityList;

    public ListVariableTracker(ListVariableDescriptor<Solution_> variableDescriptor) {
        this.variableDescriptor = variableDescriptor;
        beforeVariableChangedEntityList = new ArrayList<>();
        afterVariableChangedEntityList = new ArrayList<>();
    }

    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return variableDescriptor;
    }

    public void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector) {
        beforeVariableChangedEntityList.clear();
        afterVariableChangedEntityList.clear();
    }

    public void close() {
        // No need to do anything for stateless implementations.
    }

    public void beforeListVariableChanged(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, int fromIndex,
            int toIndex) {
        beforeVariableChangedEntityList.add(entity);
    }

    public void afterListVariableChanged(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, int fromIndex,
            int toIndex) {
        afterVariableChangedEntityList.add(entity);
    }

    public List<String> getEntitiesMissingBeforeAfterEvents(
            List<VariableId<Solution_>> changedVariables) {
        List<String> out = new ArrayList<>();
        for (var changedVariable : changedVariables) {
            if (!variableDescriptor.equals(changedVariable.variableDescriptor())) {
                continue;
            }
            Object entity = changedVariable.entity();
            if (!beforeVariableChangedEntityList.contains(entity)) {
                out.add("Entity (" + entity
                        + ") is missing a beforeListVariableChanged call for list variable ("
                        + variableDescriptor.getVariableName() + ").");
            }
            if (!afterVariableChangedEntityList.contains(entity)) {
                out.add("Entity (" + entity
                        + ") is missing a afterListVariableChanged call for list variable ("
                        + variableDescriptor.getVariableName() + ").");
            }
        }
        beforeVariableChangedEntityList.clear();
        afterVariableChangedEntityList.clear();
        return out;
    }

    public TrackerDemand demand() {
        return new TrackerDemand();
    }

    public void afterListElementUnassigned(InnerScoreDirector<Solution_, ?> scoreDirector, Object unassignedElement) {
        // Do nothing
    }

    /**
     * In order for the {@link ListVariableTracker} to be registered for shadow variable update events,
     * it needs to be passed to the {@link InnerScoreDirector#getSupplyManager()}, which requires a {@link Demand}.
     * <p>
     * Unlike most other {@link Demand}s, there will only be one instance of
     * {@link ListVariableTracker} in the {@link InnerScoreDirector} for each list variable.
     */
    public class TrackerDemand implements Demand<ListVariableTracker<Solution_>> {
        @Override
        public ListVariableTracker<Solution_> createExternalizedSupply(SupplyManager supplyManager) {
            return ListVariableTracker.this;
        }
    }
}
