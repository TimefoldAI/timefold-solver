package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ExternalizedSingletonListListInverseVariableProcessor<Solution_>
        implements SingletonListInverseVariableProcessor<Solution_> {

    private final InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    private final ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    public ExternalizedSingletonListListInverseVariableProcessor(
            InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
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
            throw new IllegalStateException("The entity (" + inverseEntity
                    + ") has a list variable (" + sourceVariableDescriptor.getVariableName()
                    + ") and one of its elements (" + element
                    + ") which has a shadow variable (" + shadowVariableDescriptor.getVariableName()
                    + ") has an oldInverseEntity (" + oldInverseEntity + ") which is not that entity.\n"
                    + "Verify the consistency of your input problem for that shadow variable.");
        }
        setInverse(scoreDirector, inverseEntity, element);
    }

    private void setInverse(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        shadowVariableDescriptor.setValue(element, entity);
        scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
    }

    @Override
    public void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        setInverseAsserted(scoreDirector, element, null, entity);
    }

    @Override
    public void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        setInverse(scoreDirector, null, element);
    }

    @Override
    public void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, Object element) {
        if (getInverseSingleton(element) != entity) {
            setInverse(scoreDirector, entity, element);
        }
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }
}
