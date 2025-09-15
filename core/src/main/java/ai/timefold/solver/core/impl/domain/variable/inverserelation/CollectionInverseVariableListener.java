package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.util.Collection;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ChangeEventType;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public class CollectionInverseVariableListener<Solution_>
        implements InnerVariableListener<Solution_, BasicVariableChangeEvent<Object>>,
        CollectionInverseVariableSupply {

    protected final InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    protected final VariableDescriptor<Solution_> sourceVariableDescriptor;

    public CollectionInverseVariableListener(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            VariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public ChangeEventType listenedEventType() {
        return ChangeEventType.BASIC;
    }

    @Override
    public void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector) {
        InnerVariableListener.forEachEntity(scoreDirector, shadowVariableDescriptor.getEntityDescriptor().getEntityClass(),
                value -> getInverseCollection(value).clear());
        InnerVariableListener.forEachEntity(scoreDirector, sourceVariableDescriptor.getEntityDescriptor().getEntityClass(),
                entity -> insert(scoreDirector, entity));
    }

    @Override
    public void beforeChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            BasicVariableChangeEvent<Object> event) {
        retract(scoreDirector, event.entity());
    }

    @Override
    public void afterChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            BasicVariableChangeEvent<Object> event) {
        insert(scoreDirector, event.entity());
    }

    protected void insert(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity) {
        Object shadowEntity = sourceVariableDescriptor.getValue(entity);
        if (shadowEntity != null) {
            Collection<Object> shadowCollection = shadowVariableDescriptor.getValue(shadowEntity);
            if (scoreDirector.expectShadowVariablesInCorrectState() && shadowCollection == null) {
                throw new IllegalStateException("""
                        The entity (%s) has a variable (%s) with value (%s) which has a sourceVariableName variable (%s) \
                        with a value (%s) which is null.
                        Verify the consistency of your input problem for that bi-directional relationship.
                        Non-singleton inverse variable can never be null, at the very least it should be an empty %s."""
                        .formatted(entity, sourceVariableDescriptor.getVariableName(), shadowEntity,
                                shadowVariableDescriptor.getVariableName(), shadowCollection,
                                Collection.class.getSimpleName()));
            }
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, shadowEntity);
            boolean added = shadowCollection.add(entity);
            if (scoreDirector.expectShadowVariablesInCorrectState() && !added) {
                throw new IllegalStateException("""
                        The entity (%s) has a variable (%s) with value (%s) which has a sourceVariableName variable (%s) \
                        with a value (%s) which already contained the entity (%s).
                        Verify the consistency of your input problem for that bi-directional relationship."""
                        .formatted(entity, sourceVariableDescriptor.getVariableName(), shadowEntity,
                                shadowVariableDescriptor.getVariableName(), shadowCollection, entity));
            }
            scoreDirector.afterVariableChanged(shadowVariableDescriptor, shadowEntity);
        }
    }

    protected void retract(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity) {
        Object shadowEntity = sourceVariableDescriptor.getValue(entity);
        if (shadowEntity != null) {
            Collection<Object> shadowCollection = shadowVariableDescriptor.getValue(shadowEntity);
            if (scoreDirector.expectShadowVariablesInCorrectState() && shadowCollection == null) {
                throw new IllegalStateException("""
                        The entity (%s) has a variable (%s) with value (%s) which has a sourceVariableName variable (%s) \
                        with a value (%s) which is null.
                        Verify the consistency of your input problem for that bi-directional relationship.
                        Non-singleton inverse variable can never be null, at the very least it should be an empty %s."""
                        .formatted(entity, sourceVariableDescriptor.getVariableName(), shadowEntity,
                                shadowVariableDescriptor.getVariableName(), shadowCollection,
                                Collection.class.getSimpleName()));
            }
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, shadowEntity);
            boolean removed = shadowCollection.remove(entity);
            if (scoreDirector.expectShadowVariablesInCorrectState() && !removed) {
                throw new IllegalStateException("""
                        The entity (%s) has a variable (%s) with value (%s) which has a sourceVariableName variable (%s) \
                        with a value (%s) which did not contain the entity (%s)
                        Verify the consistency of your input problem for that bi-directional relationship."""
                        .formatted(entity, sourceVariableDescriptor.getVariableName(), shadowEntity,
                                shadowVariableDescriptor.getVariableName(), shadowCollection, entity));
            }
            scoreDirector.afterVariableChanged(shadowVariableDescriptor, shadowEntity);
        }
    }

    @Override
    public Collection<?> getInverseCollection(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }

}
