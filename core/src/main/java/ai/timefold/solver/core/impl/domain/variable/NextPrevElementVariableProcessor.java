package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

sealed interface NextPrevElementVariableProcessor<Solution_>
        permits AbstractExternalizedNextPrevElementVariableProcessor, InternalNextPrevVariableProcessor {

    void setElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element, int index);

    void unsetElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    Object getElement(Object element);

}
