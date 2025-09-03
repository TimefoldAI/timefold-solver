package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class EntityConsistencyState<Solution_> implements Supply {
    @Nullable
    private final IdentityHashMap<Object, Boolean> entityToIsInconsistentMap;

    @Nullable
    private final ExternalizedShadowVariableInconsistentProcessor<Solution_> externalizedShadowVariableInconsistentProcessor;

    EntityConsistencyState(EntityDescriptor<Solution_> entityDescriptor) {
        var entityIsInconsistentDescriptor = entityDescriptor.getShadowVariablesInconsistentDescriptor();

        if (entityIsInconsistentDescriptor == null) {
            entityToIsInconsistentMap = new IdentityHashMap<>();
            externalizedShadowVariableInconsistentProcessor = null;
        } else {
            entityToIsInconsistentMap = null;
            externalizedShadowVariableInconsistentProcessor =
                    new ExternalizedShadowVariableInconsistentProcessor<>(entityIsInconsistentDescriptor);
        }
    }

    public static <Solution_> EntityConsistencyState<Solution_> of(EntityDescriptor<Solution_> entityDescriptor) {
        throw new UnsupportedOperationException();
    }

    public boolean isEntityConsistent(Object entity) {
        if (externalizedShadowVariableInconsistentProcessor != null) {
            return Boolean.FALSE.equals(externalizedShadowVariableInconsistentProcessor.getIsEntityInconsistent(entity));
        }
        return Boolean.FALSE.equals(entityToIsInconsistentMap.get(entity));
    }

    public Boolean getEntityInconsistentValue(Object entity) {
        if (externalizedShadowVariableInconsistentProcessor != null) {
            return externalizedShadowVariableInconsistentProcessor.getIsEntityInconsistent(entity);
        }
        return entityToIsInconsistentMap.get(entity);
    }

    public void setEntityIsInconsistent(ChangedVariableNotifier<Solution_> changedVariableNotifier,
            VariableDescriptor<Solution_> randomDeclarativeVariableDescriptor, Object entity,
            boolean isInconsistent) {
        if (externalizedShadowVariableInconsistentProcessor != null) {
            externalizedShadowVariableInconsistentProcessor.setIsEntityInconsistent(changedVariableNotifier, entity,
                    isInconsistent);
            return;
        }
        // There is no ShadowVariablesInconsistent shadow variable,
        // so we use a random declarative shadow variable on the entity to notify the score director
        // that the entity changed.
        //
        // Since declarative shadow variables cannot be used as sources for legacy variable listeners,
        // this does not cause any issues/additional recalculation of shadow variables.
        changedVariableNotifier.beforeVariableChanged().accept(randomDeclarativeVariableDescriptor, entity);
        entityToIsInconsistentMap.put(entity, isInconsistent);
        changedVariableNotifier.afterVariableChanged().accept(randomDeclarativeVariableDescriptor, entity);
    }

}
