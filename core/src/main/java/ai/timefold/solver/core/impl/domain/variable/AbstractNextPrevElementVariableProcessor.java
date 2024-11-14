package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.LocationInList;

abstract sealed class AbstractNextPrevElementVariableProcessor<Solution_>
        permits NextElementVariableProcessor, PreviousElementVariableProcessor {

    public abstract void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable,
            Object originalElement, LocationInList originalElementLocation);

    public abstract void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    public abstract void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable,
            Object originalElement, LocationInList originalElementLocation);

}
