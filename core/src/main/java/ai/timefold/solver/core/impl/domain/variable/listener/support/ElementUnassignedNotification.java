package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.domain.variable.ListElementUnassignedChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ListVariableChangeEvent;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ElementUnassignedNotification<Solution_> implements ListVariableNotification<Solution_> {

    private final Object element;

    ElementUnassignedNotification(Object element) {
        this.element = element;
    }

    @Override
    public void triggerBefore(
            InnerVariableListener<Solution_, ListVariableChangeEvent<Object, Object>> variableListener,
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        throw new UnsupportedOperationException("ListVariableListeners do not listen for this event.");
    }

    @Override
    public void triggerAfter(
            InnerVariableListener<Solution_, ListVariableChangeEvent<Object, Object>> variableListener,
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        variableListener.afterChange(scoreDirector, new ListElementUnassignedChangeEvent<>(element));
    }

    @Override
    public String toString() {
        return "ElementUnassigned(" + element + ")";
    }
}
