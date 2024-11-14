package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public sealed interface IndexVariableProcessor<Solution_> extends IndexVariableSupply
        permits InternalIndexVariableProcessor, ExternalizedIndexVariableProcessor {

    void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index);

    void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index);

}
