package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.domain.variable.ListElementsChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ListVariableChangeEvent;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class ListVariableChangedNotification<Solution_> extends AbstractNotification
        implements ListVariableNotification<Solution_> {

    private final int fromIndex;
    private final int toIndex;

    ListVariableChangedNotification(Object entity, int fromIndex, int toIndex) {
        super(entity);
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    @Override
    public void triggerBefore(
            InnerVariableListener<Solution_, ListVariableChangeEvent<Object, Object>> variableListener,
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        variableListener.beforeChange(scoreDirector,
                new ListElementsChangeEvent<>(entity, fromIndex, toIndex));
    }

    @Override
    public void triggerAfter(
            InnerVariableListener<Solution_, ListVariableChangeEvent<Object, Object>> variableListener,
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        variableListener.afterChange(scoreDirector,
                new ListElementsChangeEvent<>(entity, fromIndex, toIndex));
    }

    @Override
    public String toString() {
        return "ListVariableChangedNotification(" + entity + "[" + fromIndex + ".." + toIndex + "])";
    }
}
