package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public sealed interface SingletonListInverseVariableProcessor<Solution_> extends SingletonInverseVariableSupply
        permits InternalSingletonListListInverseVariableProcessor, ExternalizedSingletonListListInverseVariableProcessor {

    void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element);

    void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element);

    void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element);

}
