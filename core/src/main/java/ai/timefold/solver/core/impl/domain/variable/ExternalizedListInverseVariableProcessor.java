package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ExternalizedListInverseVariableProcessor<Solution_> {

    private final InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    public ExternalizedListInverseVariableProcessor(
            InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    public void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        setInverseAsserted(scoreDirector, element, entity, null);
    }

    private void setInverseAsserted(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Object inverseEntity,
            Object expectedOldInverseEntity) {
        var oldInverseEntity = getInverseSingleton(element);
        if (oldInverseEntity == inverseEntity) {
            return;
        }
        if (scoreDirector.expectShadowVariablesInCorrectState() && oldInverseEntity != expectedOldInverseEntity) {
            throw new IllegalStateException("""
                    The entity (%s) has a list variable (%s) and one of its elements (%s) which has a shadow variable (%s) \
                    has an oldInverseEntity (%s) which is not that entity.
                    Verify the consistency of your input problem for that shadow variable."""
                    .formatted(inverseEntity, sourceVariableDescriptor.getVariableName(), element,
                            shadowVariableDescriptor.getVariableName(), oldInverseEntity));
        }
        setInverse(scoreDirector, inverseEntity, element);
    }

    private void setInverse(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        shadowVariableDescriptor.setValue(element, entity);
        scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
    }

    public void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        setInverseAsserted(scoreDirector, element, null, entity);
    }

    public void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        changeElement(scoreDirector, null, element);
    }

    public void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        if (getInverseSingleton(element) != entity) {
            setInverse(scoreDirector, entity, element);
        }
    }

    public Object getInverseSingleton(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }
}
