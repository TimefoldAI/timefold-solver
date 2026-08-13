package ai.timefold.solver.core.impl.domain.variable;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

/**
 * Demands a {@link ListVariableStateSupply} on {@link #phaseStarted(AbstractPhaseScope)} and releases it on
 * {@link #phaseEnded(AbstractPhaseScope)}, re-demanding on every phase start because a phase may run under a
 * different {@link ai.timefold.solver.core.config.solver.EnvironmentMode}, which swaps in a new score director
 * (and thus a new {@link ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager}).
 * <p>
 * Intended to be held as a field by selectors that need a {@link ListVariableStateSupply} across phase lifecycle
 * events, delegating their own {@code phaseStarted}/{@code phaseEnded} overrides to this holder instead of each
 * re-implementing the demand/cancel bookkeeping.
 *
 * @param <Solution_> the solution type, the class with the {@link ai.timefold.solver.core.api.domain.solution.PlanningSolution}
 *        annotation
 */
public final class ListVariableStateSupplyHolder<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private ListVariableStateSupply<Solution_, ?, ?> listVariableStateSupply;

    public ListVariableStateSupplyHolder(ListVariableDescriptor<Solution_> listVariableDescriptor) {
        this.listVariableDescriptor = listVariableDescriptor;
    }

    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        listVariableStateSupply = phaseScope.getScoreDirector().getSupplyManager()
                .demand(listVariableDescriptor.getStateDemand());
    }

    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        phaseScope.getScoreDirector().getSupplyManager().cancel(listVariableDescriptor.getStateDemand());
        listVariableStateSupply = null;
    }

    @SuppressWarnings("unchecked")
    public <Entity_, Element_> ListVariableStateSupply<Solution_, Entity_, Element_> get() {
        return (ListVariableStateSupply<Solution_, Entity_, Element_>) Objects.requireNonNull(listVariableStateSupply,
                "Impossible state: The listVariableStateSupply is not initialized yet.");
    }
}
