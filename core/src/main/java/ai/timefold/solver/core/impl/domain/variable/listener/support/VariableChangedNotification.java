package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

final class VariableChangedNotification<Solution_> extends AbstractNotification
        implements BasicVariableNotification<Solution_> {

    VariableChangedNotification(Object entity) {
        super(entity);
    }

    @Override
    public void triggerBefore(VariableListener<Solution_, Object> variableListener, ScoreDirector<Solution_> scoreDirector) {
        variableListener.beforeVariableChanged(scoreDirector, entity);
    }

    @Override
    public void triggerAfter(VariableListener<Solution_, Object> variableListener, ScoreDirector<Solution_> scoreDirector) {
        variableListener.afterVariableChanged(scoreDirector, entity);
    }

    @Override
    public String toString() {
        return "VariableChanged(" + entity + ")";
    }
}
