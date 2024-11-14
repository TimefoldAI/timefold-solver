package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * Has two implementations:
 *
 * <ul>
 * <li>{@link ExternalizedSingletonListListInverseVariableProcessor} uses the shadow variable declared in user's data
 * model.</li>
 * <li>{@link InternalSingletonListListInverseVariableProcessor} uses our internal tracker when the shadow variable isn't
 * present.</li>
 * </ul>
 *
 * @param <Solution_>
 */
sealed interface SingletonListInverseVariableProcessor<Solution_> extends SingletonInverseVariableSupply
        permits ExternalizedSingletonListListInverseVariableProcessor, InternalSingletonListListInverseVariableProcessor {

    void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element);

    void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element);

    void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element);

}
