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
    @Nullable
    protected ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply;

    protected AbstractListMoveSelector(ListVariableDescriptor<Solution_> listVariableDescriptor) {
        this.listVariableDescriptor = listVariableDescriptor;
    }

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
