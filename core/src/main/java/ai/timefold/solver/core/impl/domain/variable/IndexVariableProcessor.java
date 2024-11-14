package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * Has two implementations:
 * 
 * <ul>
 * <li>{@link ExternalizedIndexVariableProcessor} uses the shadow variable declared in user's data model.</li>
 * <li>{@link InternalIndexVariableProcessor} uses our internal tracker when the shadow variable isn't present.</li>
 * </ul>
 * 
 * @param <Solution_>
 */
sealed interface IndexVariableProcessor<Solution_> extends IndexVariableSupply
        permits ExternalizedIndexVariableProcessor, InternalIndexVariableProcessor {

    void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index);

    void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element);

    void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index);

}
