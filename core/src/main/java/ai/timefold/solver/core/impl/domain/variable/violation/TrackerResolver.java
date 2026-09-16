package ai.timefold.solver.core.impl.domain.variable.violation;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

public interface TrackerResolver<Solution_> {

    /**
     * Returns the {@link BasicVariableTracker} used to detect missing or incorrect variable
     * listener notifications for the given basic {@link PlanningVariable}.
     * Used by {@link EnvironmentMode#TRACKED_FULL_ASSERT}.
     *
     * @param variableDescriptor never null, must not describe a {@link PlanningListVariable}
     * @return never null
     */
    BasicVariableTracker<Solution_> getBasicVariableTracker(VariableDescriptor<Solution_> variableDescriptor);

    /**
     * Returns the {@link ListVariableTracker} used to detect missing or incorrect variable
     * listener notifications for the given {@link PlanningListVariable}.
     * Used by {@link EnvironmentMode#TRACKED_FULL_ASSERT}.
     *
     * @param variableDescriptor never null
     * @return never null
     */
    ListVariableTracker<Solution_> getListVariableTracker(ListVariableDescriptor<Solution_> variableDescriptor);
}
