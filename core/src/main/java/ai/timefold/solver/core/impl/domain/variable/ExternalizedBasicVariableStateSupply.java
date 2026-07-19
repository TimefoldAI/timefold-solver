package ai.timefold.solver.core.impl.domain.variable;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ExternalizedBasicVariableStateSupply<Solution_> implements BasicVariableStateSupply<Solution_> {

    private final VariableDescriptor<Solution_> sourceVariableDescriptor;
    private final Consumer<Object> notifier;

    @Nullable
    private InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    @Nullable
    private Map<Object, Set<Object>> inverseEntitySetMap;

    public ExternalizedBasicVariableStateSupply(VariableDescriptor<Solution_> sourceVariableDescriptor,
            Consumer<Object> notifier) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
        this.notifier = notifier;
    }

    @Override
    public void externalize(InverseRelationShadowVariableDescriptor<Solution_> descriptor) {
        if (shadowVariableDescriptor != null) {
            throw new IllegalStateException(
                    "Impossible state: the sourceVariableDescriptor (%s) is already externalized by (%s); cannot externalize by (%s) as well."
                            .formatted(sourceVariableDescriptor, shadowVariableDescriptor, descriptor));
        }
        shadowVariableDescriptor = descriptor;
    }

    @Override
    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return sourceVariableDescriptor;
    }

    @Override
    public void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector) {
        if (shadowVariableDescriptor == null) {
            inverseEntitySetMap = new IdentityHashMap<>();
            sourceVariableDescriptor.getEntityDescriptor().visitAllEntities(scoreDirector.getWorkingSolution(),
                    entity -> insert(scoreDirector, entity));
        } else {
            forEachEntity(scoreDirector, shadowVariableDescriptor.getEntityDescriptor().getEntityClass(),
                    value -> getInverseCollection(value).clear());
            forEachEntity(scoreDirector, sourceVariableDescriptor.getEntityDescriptor().getEntityClass(),
                    entity -> insert(scoreDirector, entity));
        }
    }

    @SuppressWarnings("unchecked")
    private static <Solution_, Entity_> void forEachEntity(InnerScoreDirector<Solution_, ?> scoreDirector,
            Class<? extends Entity_> entityClass,
            Consumer<Entity_> entityConsumer) {
        scoreDirector.getSolutionDescriptor().visitEntitiesByEntityClass(scoreDirector.getWorkingSolution(),
                entityClass, entity -> {
                    entityConsumer.accept((Entity_) entity);
                    return false;
                });
    }

    @Override
    public void beforeVariableChanged(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity) {
        retract(scoreDirector, entity);
    }

    @Override
    public void afterVariableChanged(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity) {
        insert(scoreDirector, entity);
    }

    @Override
    public void close() {
        if (shadowVariableDescriptor == null) {
            inverseEntitySetMap = null;
        }
    }

    private void insert(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity) {
        if (shadowVariableDescriptor == null) {
            Object value = sourceVariableDescriptor.getValue(entity);
            if (value == null) {
                return;
            }
            Set<Object> inverseEntitySet = inverseEntitySetMap.computeIfAbsent(value,
                    k -> Collections.newSetFromMap(new IdentityHashMap<>()));
            boolean addSucceeded = inverseEntitySet.add(entity);
            if (!addSucceeded) {
                throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                        + " because the entity (" + entity
                        + ") for sourceVariable (" + sourceVariableDescriptor.getVariableName()
                        + ") cannot be inserted: it was already inserted.");
            }
            notifier.accept(value);
        } else {
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
    }

    private void retract(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity) {
        if (shadowVariableDescriptor == null) {
            Object value = sourceVariableDescriptor.getValue(entity);
            if (value == null) {
                return;
            }
            Set<Object> inverseEntitySet = inverseEntitySetMap.get(value);
            boolean removeSucceeded = inverseEntitySet.remove(entity);
            if (!removeSucceeded) {
                throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                        + " because the entity (" + entity
                        + ") for sourceVariable (" + sourceVariableDescriptor.getVariableName()
                        + ") cannot be retracted: it was never inserted.");
            }
            if (inverseEntitySet.isEmpty()) {
                inverseEntitySetMap.put(value, null);
            }
            notifier.accept(value);
        } else {
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
    }

    @Override
    public Collection<?> getInverseCollection(Object planningValue) {
        if (shadowVariableDescriptor == null) {
            Set<Object> inverseEntitySet = inverseEntitySetMap.get(planningValue);
            if (inverseEntitySet == null) {
                return Collections.emptySet();
            }
            return inverseEntitySet;
        } else {
            return shadowVariableDescriptor.getValue(planningValue);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceVariableDescriptor.getVariableName() + ")";
    }

}
