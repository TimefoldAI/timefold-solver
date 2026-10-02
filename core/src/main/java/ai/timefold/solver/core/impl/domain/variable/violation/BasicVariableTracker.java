package ai.timefold.solver.core.impl.domain.variable.violation;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeHandler;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NonNull;

/**
 * Tracks variable change events for a given genuine or shadow variable, except {@link PlanningListVariable}.
 */
public final class BasicVariableTracker<Solution_> implements BasicVariableChangeHandler<Solution_> {
    private final VariableDescriptor<Solution_> variableDescriptor;
    private final List<Object> beforeVariableChangedEntityList;
    private final List<Object> afterVariableChangedEntityList;

    public BasicVariableTracker(VariableDescriptor<Solution_> variableDescriptor) {
        this.variableDescriptor = variableDescriptor;
        beforeVariableChangedEntityList = new ArrayList<>();
        afterVariableChangedEntityList = new ArrayList<>();
    }

    @Override
    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return variableDescriptor;
    }

    @Override
    public void resetWorkingSolution(@NonNull InnerScoreDirector<Solution_, ?> scoreDirector) {
        beforeVariableChangedEntityList.clear();
        afterVariableChangedEntityList.clear();
    }

    @Override
    public void beforeVariableChanged(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity) {
        beforeVariableChangedEntityList.add(entity);
    }

    @Override
    public void afterVariableChanged(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity) {
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
                out.add("Entity (" + entity + ") is missing a beforeVariableChanged call for variable ("
                        + variableDescriptor.getVariableName() + ").");
            }
            if (!afterVariableChangedEntityList.contains(entity)) {
                out.add("Entity (" + entity + ") is missing a afterVariableChanged call for variable ("
                        + variableDescriptor.getVariableName() + ").");
            }
        }
        beforeVariableChangedEntityList.clear();
        afterVariableChangedEntityList.clear();
        return out;
    }
}
