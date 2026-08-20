package ai.timefold.solver.core.impl.domain.variable;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

/**
 * Borrows the {@link ListVariableStateSupply} owned by the score director,
 * re-reading it on every {@link #phaseStarted(AbstractPhaseScope)}
 * because a phase may run under a different {@link EnvironmentMode},
 * which swaps in a new score director (and thus a new supply instance).
 * <p>
 * This holder neither demands nor cancels the supply.
 * The score director demands it once on construction and cancels it on close,
 * so {@link #phaseEnded(AbstractPhaseScope)} only drops the borrowed reference.
 * Sharing the score director's instance also guarantees selectors observe exactly the same list variable state the score
 * calculation does.
 * <p>
 * Intended to be held as a field by selectors that need a {@link ListVariableStateSupply} across phase lifecycle events,
 * delegating their own {@code phaseStarted}/{@code phaseEnded} overrides to this holder instead of each re-implementing the
 * bookkeeping.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution}
 *        annotation
 */
public final class ListVariableStateSupplyHolder<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private ListVariableStateSupply<Solution_, ?, ?> listVariableStateSupply;

    public ListVariableStateSupplyHolder(ListVariableDescriptor<Solution_> listVariableDescriptor) {
        this.listVariableDescriptor = listVariableDescriptor;
    }

    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // We reuse the state owned by the score director, rather than demanding a second supply of our own.
        this.listVariableStateSupply = phaseScope.getScoreDirector().getListVariableStateSupply(listVariableDescriptor);
    }

    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // There's no need to release the state, as the score director will take care of it.
        listVariableStateSupply = null;
    }

    @SuppressWarnings("unchecked")
    public <Entity_, Element_> ListVariableStateSupply<Solution_, Entity_, Element_> get() {
        return (ListVariableStateSupply<Solution_, Entity_, Element_>) Objects.requireNonNull(listVariableStateSupply,
                "Impossible state: The listVariableStateSupply is not initialized yet.");
    }
}
