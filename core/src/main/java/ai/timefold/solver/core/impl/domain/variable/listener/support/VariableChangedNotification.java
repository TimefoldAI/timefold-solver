package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.InnerBasicVariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class VariableChangedNotification<Solution_> extends AbstractNotification
        implements BasicVariableNotification<Solution_> {

    VariableChangedNotification(Object entity) {
        super(entity);
    }

    @Override
    public void triggerBefore(
            InnerBasicVariableListener<Solution_, Object> variableListener,
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        variableListener.beforeChange(scoreDirector, new BasicVariableChangeEvent<>(entity));
    }

    @Override
    public void triggerAfter(
            InnerBasicVariableListener<Solution_, Object> variableListener,
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        variableListener.afterChange(scoreDirector, new BasicVariableChangeEvent<>(entity));
    }

    @Override
    public String toString() {
        return "VariableChanged(" + entity + ")";
    }
}
