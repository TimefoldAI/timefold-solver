package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

abstract sealed class AbstractNextPrevElementVariableProcessor<Solution_>
        permits NextElementVariableProcessor, PreviousElementVariableProcessor {

    public abstract void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            int index);

    public abstract void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    public abstract void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable,
            Object element, int index);

}
