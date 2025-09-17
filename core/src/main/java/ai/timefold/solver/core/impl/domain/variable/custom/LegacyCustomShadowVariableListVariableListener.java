package ai.timefold.solver.core.impl.domain.variable.custom;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.InnerListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.domain.variable.ListElementsChangeEvent;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LegacyCustomShadowVariableListVariableListener<Solution_, Entity_, Element_>(
        Class<? extends Entity_> sourceEntityClass,
        ListVariableListener<Solution_, Entity_, Element_> customVariableListener)
        implements
            InnerListVariableListener<Solution_, Entity_, Element_> {

    @Override
    public void beforeChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            ListElementsChangeEvent<Entity_> changeEvent) {
        customVariableListener.beforeListVariableChanged(scoreDirector,
                changeEvent.entity(),
                changeEvent.changeStartIndexInclusive(),
                changeEvent.changeEndIndexExclusive());
    }

    @Override
    public void afterChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            ListElementsChangeEvent<Entity_> changeEvent) {
        customVariableListener.afterListVariableChanged(scoreDirector,
                changeEvent.entity(),
                changeEvent.changeStartIndexInclusive(),
                changeEvent.changeEndIndexExclusive());
    }

    @Override
    public void afterListElementUnassigned(InnerScoreDirector<Solution_, ?> scoreDirector, Element_ unassignedElement) {
        customVariableListener.afterListVariableElementUnassigned(scoreDirector, unassignedElement);
    }

    @Override
    public void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector) {
        customVariableListener.resetWorkingSolution(scoreDirector);
        InnerVariableListener.forEachEntity(scoreDirector,
                sourceEntityClass,
                entity -> {
                    customVariableListener.beforeEntityAdded(scoreDirector, entity);
                    customVariableListener.afterEntityAdded(scoreDirector, entity);
                });
    }

    @Override
    public void close() {
        customVariableListener.close();
    }

    @Override
    public String toString() {
        return customVariableListener.toString();
    }
}
