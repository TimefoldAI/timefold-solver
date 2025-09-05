package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class EntityConsistencyState<Solution_, Entity_> {
    private final IdentityHashMap<Entity_, Boolean> entityToIsInconsistentMap;

    @Nullable
    private final ExternalizedShadowVariableInconsistentProcessor<Solution_> externalizedShadowVariableInconsistentProcessor;

    private final VariableDescriptor<Solution_> randomDeclarativeVariableDescriptor;

    EntityConsistencyState(EntityDescriptor<Solution_> entityDescriptor,
            IdentityHashMap<Entity_, Boolean> entityToIsInconsistentMap) {
        this.entityToIsInconsistentMap = entityToIsInconsistentMap;

        var entityIsInconsistentDescriptor = entityDescriptor.getShadowVariablesInconsistentDescriptor();
        if (entityIsInconsistentDescriptor == null) {
            externalizedShadowVariableInconsistentProcessor = null;
            randomDeclarativeVariableDescriptor = entityDescriptor.getShadowVariableDescriptors()
                    .stream()
                    .filter(variableDescriptor -> variableDescriptor instanceof DeclarativeShadowVariableDescriptor<?>)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Impossible state: Entity class (%s) does not have any declarative variable descriptors."
                                    .formatted(entityDescriptor.getEntityClass())));
        } else {
            externalizedShadowVariableInconsistentProcessor =
                    new ExternalizedShadowVariableInconsistentProcessor<>(entityIsInconsistentDescriptor);
            randomDeclarativeVariableDescriptor = entityIsInconsistentDescriptor;
        }
    }

    public boolean isEntityConsistent(Entity_ entity) {
        return Boolean.FALSE.equals(entityToIsInconsistentMap.get(entity));
    }

    @Nullable
    Boolean getEntityInconsistentValue(Entity_ entity) {
        return entityToIsInconsistentMap.get(entity);
    }

    public void setEntityIsInconsistent(ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Entity_ entity,
            boolean isInconsistent) {
        if (externalizedShadowVariableInconsistentProcessor != null) {
            externalizedShadowVariableInconsistentProcessor.setIsEntityInconsistent(changedVariableNotifier, entity,
                    isInconsistent);
        }
        // There may be no ShadowVariablesInconsistent shadow variable,
        // so we use a random declarative shadow variable on the entity to notify the score director
        // that the entity changed.
        //
        // Since declarative shadow variables cannot be used as sources for legacy variable listeners,
        // this does not cause any issues/additional recalculation of shadow variables.
        changedVariableNotifier.beforeVariableChanged().accept(randomDeclarativeVariableDescriptor, entity);
        entityToIsInconsistentMap.put(entity, isInconsistent);
        changedVariableNotifier.afterVariableChanged().accept(randomDeclarativeVariableDescriptor, entity);
    }

    @Nullable
    Boolean getEntityInconsistentValueFromProcessorOrNull(Entity_ entity) {
        if (externalizedShadowVariableInconsistentProcessor != null) {
            return externalizedShadowVariableInconsistentProcessor.getIsEntityInconsistent(entity);
        }
        return null;
    }

    public void setEntityIsInconsistentSkippingProcessor(Entity_ entity,
            boolean isInconsistent) {
        entityToIsInconsistentMap.put(entity, isInconsistent);
    }

}
