package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * Serves for detecting undo move corruption. When a snapshot is created, it records the state of all variables (genuine and
 * shadow) for all entities.
 */
public class AllVariablesAssert<Solution_> {
    private final Map<VariableId<Solution_>, VariableSnapshot<Solution_>> variableIdToSnapshot =
            new HashMap<>();

    public static <Solution_> AllVariablesAssert<Solution_> takeSnapshot(
            SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ workingSolution) {
        AllVariablesAssert<Solution_> out = new AllVariablesAssert<>();
        solutionDescriptor.visitAllEntities(workingSolution, entity -> {
            EntityDescriptor<Solution_> entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            for (VariableDescriptor<Solution_> variableDescriptor : entityDescriptor.getDeclaredVariableDescriptors()) {
                out.recordVariable(solutionDescriptor, variableDescriptor, entity);
            }
        });
        return out;
    }

    private void recordVariable(SolutionDescriptor<Solution_> solutionDescriptor,
            VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        VariableSnapshot<Solution_> snapshot = new VariableSnapshot<>(solutionDescriptor, variableDescriptor, entity);
        variableIdToSnapshot.put(snapshot.getVariableId(), snapshot);
    }

    public List<VariableId<Solution_>> changedVariablesFrom(AllVariablesAssert<Solution_> before) {
        List<VariableId<Solution_>> out = new ArrayList<>();
        for (VariableId<Solution_> variableId : variableIdToSnapshot.keySet()) {
            VariableSnapshot<Solution_> variableBefore = before.variableIdToSnapshot.get(variableId);
            VariableSnapshot<Solution_> variableAfter = this.variableIdToSnapshot.get(variableId);
            if (variableBefore.isDifferentFrom(variableAfter)) {
                out.add(variableAfter.getVariableId());
            }
        }
        return out;
    }

    public VariableSnapshot<Solution_> getVariableSnapshot(VariableId<Solution_> variableId) {
        return variableIdToSnapshot.get(variableId);
    }
}
