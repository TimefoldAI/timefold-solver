package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public class NormalVariableTracker<Solution_>
        implements SourcedVariableListener<Solution_>, VariableListener<Solution_, Object>, Supply {
    private final VariableDescriptor<Solution_> variableDescriptor;
    private final List<Object> beforeVariableChangedEntityList;
    private final List<Object> afterVariableChangedEntityList;

    public NormalVariableTracker(VariableDescriptor<Solution_> variableDescriptor) {
        this.variableDescriptor = variableDescriptor;
        beforeVariableChangedEntityList = new ArrayList<>();
        afterVariableChangedEntityList = new ArrayList<>();
    }

    @Override
    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return variableDescriptor;
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object object) {

    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object object) {

    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object object) {

    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object object) {

    }

    @Override
    public void resetWorkingSolution(ScoreDirector<Solution_> scoreDirector) {
        beforeVariableChangedEntityList.clear();
        afterVariableChangedEntityList.clear();
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        beforeVariableChangedEntityList.add(entity);
    }

    @Override
    public void afterVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
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

    public TrackerDemand demand() {
        return new TrackerDemand();
    }

    public class TrackerDemand implements Demand<NormalVariableTracker<Solution_>> {
        @Override
        public NormalVariableTracker<Solution_> createExternalizedSupply(SupplyManager supplyManager) {
            return NormalVariableTracker.this;
        }
    }
}
