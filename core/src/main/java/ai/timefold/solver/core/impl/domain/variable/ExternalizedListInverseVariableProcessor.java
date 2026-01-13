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

    public boolean addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        return setInverseAsserted(scoreDirector, element, entity, null);
    }

    private boolean setInverseAsserted(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Object inverseEntity,
            Object expectedOldInverseEntity) {
        var oldInverseEntity = getInverseSingleton(element);
        if (oldInverseEntity == inverseEntity) {
            return false;
        }
        if (scoreDirector.expectShadowVariablesInCorrectState() && oldInverseEntity != expectedOldInverseEntity) {
            throw new IllegalStateException("""
                    The entity (%s) has a list variable (%s) and one of its elements (%s) which has a shadow variable (%s) \
                    has an oldInverseEntity (%s) which is not that entity.
                    Verify the consistency of your input problem for that shadow variable."""
                    .formatted(inverseEntity, sourceVariableDescriptor.getVariableName(), element,
                            shadowVariableDescriptor.getVariableName(), oldInverseEntity));
        }
        return setInverse(scoreDirector, inverseEntity, element);
    }

    private boolean setInverse(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        shadowVariableDescriptor.setValue(element, entity);
        scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
        return true;
    }

    public boolean unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        return changeElement(scoreDirector, null, element);
    }

    public boolean changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        if (getInverseSingleton(element) != entity) {
            return setInverse(scoreDirector, entity, element);
        }
        return false;
    }

    public Object getInverseSingleton(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }
}
