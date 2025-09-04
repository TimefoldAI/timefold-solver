package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.HashMap;
import java.util.IdentityHashMap;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConsistencyTracker<Solution_> {
    private final IdentityHashMap<Object, Boolean> entityToIsInconsistentMap = new IdentityHashMap<>();
    private final HashMap<Class<?>, EntityConsistencyState<Solution_>> entityClassToConsistencyStateMap = new HashMap<>();

    public EntityConsistencyState<Solution_>
            getDeclarativeEntityConsistencyState(EntityDescriptor<Solution_> entityDescriptor) {
        return entityClassToConsistencyStateMap.computeIfAbsent(entityDescriptor.getEntityClass(),
                ignored -> new EntityConsistencyState<>(entityDescriptor, entityToIsInconsistentMap));
    }
}
