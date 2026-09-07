package ai.timefold.solver.core.impl.heuristic.selector.list;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractListMoveSelector<Solution_> extends AbstractSelector<Solution_> {

    protected final ListVariableDescriptor<Solution_> listVariableDescriptor;

    /**
     * Non-null for the duration of a phase, null outside one;
     * see {@link #phaseStarted(AbstractPhaseScope)} and {@link #phaseEnded(AbstractPhaseScope)}.
     * <p>
     * Subclasses read this field directly rather than through {@link #getListVariableStateSupply()},
     * and that is deliberate: the accessor's null check would sit on the selection path,
     * where a selector's {@code iterator()} is not necessarily called only once per step,
     * and some subclasses read the supply once per selected element.
     * Nothing enforces that the field is non-null when they read it —
     * the normal path simply never selects before the phase has started.
     */
    @Nullable
    protected ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply;

    protected AbstractListMoveSelector(ListVariableDescriptor<Solution_> listVariableDescriptor) {
        this.listVariableDescriptor = listVariableDescriptor;
    }

    /**
     * For callers off the selection path,
     * where naming the cause of a missing supply is worth the null check;
     * selection code reads {@link #listVariableStateSupply} directly instead.
     */
    protected ListVariableStateSupply<Solution_, Object, Object> getListVariableStateSupply() {
        return Objects.requireNonNull(listVariableStateSupply,
                "Impossible state: The listVariableStateSupply is not initialized yet.");
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        // We reuse the state owned by the score director, rather than demanding a second supply of our own.
        this.listVariableStateSupply = phaseScope.getScoreDirector().getListVariableStateSupply(listVariableDescriptor);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        // There's no need to release the state, as the score director will take care of it.
        listVariableStateSupply = null;
    }
}
