package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.FilteringEntityByEntitySelector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NullMarked;

/**
 * This class records the relationship between each entity and all its reachable entities.
 *
 * @see FilteringEntityByEntitySelector
 */
public class ReachableEntities<Solution_> {

    private final Map<Object, ReachableItemEntity> entities;
    private final EntityDescriptor<Solution_> entityDescriptor;
    private final List<Object> allEntities;
    private final ValueRangeManager<Solution_> valueRangeManager;

    private ReachableItemEntity cachedEntity = null;

    public ReachableEntities(EntityDescriptor<Solution_> entityDescriptor, List<Object> allEntities,
            ValueRangeManager<Solution_> valueRangeManager) {
        this.entityDescriptor = entityDescriptor;
        this.allEntities = allEntities;
        this.entities = new IdentityHashMap<>(allEntities.size());
        this.valueRangeManager = valueRangeManager;
    }

    public boolean isReachable(Object entity, Object otherEntity) {
        if (!Objects.requireNonNull(entity).getClass().isAssignableFrom(entityDescriptor.getEntityClass())) {
            throw new IllegalArgumentException(
                    "Impossible state: the entity class %s does not match with the expected class %s."
                            .formatted(entity.getClass(), entityDescriptor.getEntityClass()));
        }
        var reachableItemEntity = fetchEntity(entity);
        return reachableItemEntity.entitySet().contains(otherEntity);
    }

    public List<Object> extractEntitiesAsList(Object entity) {
        var item = fetchEntity(entity);
        return item.randomAccessEntityList();
    }

    private ReachableItemEntity fetchEntity(Object entity) {
        if (cachedEntity == null || cachedEntity.entity() != entity) {
            this.cachedEntity = entities.get(Objects.requireNonNull(entity));
            if (cachedEntity == null) {
                var reachableEntities = generateReachableEntities(entity);
                var item = new ReachableItemEntity(entity, reachableEntities.key(), reachableEntities.value());
                entities.put(entity, item);
                this.cachedEntity = item;
            }
        }
        return cachedEntity;
    }

    private Pair<Set<Object>, List<Object>> generateReachableEntities(Object entity) {
        var entitiesSet = new LinkedHashSet<>(allEntities.size());
        var entitiesList = new ArrayList<>(allEntities.size());
        var valueRangeDescriptorList = entityDescriptor.getGenuineVariableDescriptorList().stream()
                .map(GenuineVariableDescriptor::getValueRangeDescriptor)
                .filter(v -> !v.canExtractValueRangeFromSolution())
                .toList();
        // An entity can be reached by another entity
        // if they share at least one common value in the range
        for (var descriptor : valueRangeDescriptorList) {
            var entityRange = valueRangeManager.getFromEntity(descriptor, entity);
            for (var otherEntity : allEntities) {
                if (entity == otherEntity) {
                    continue;
                }
                var otherEntityRange = valueRangeManager.getFromEntity(descriptor, otherEntity);
                var otherEntityIterator = otherEntityRange.createOriginalIterator();
                while (otherEntityIterator.hasNext()) {
                    var value = otherEntityIterator.next();
                    if (value != null && entityRange.contains(value)) {
                        if (entitiesSet.add(otherEntity)) {
                            entitiesList.add(otherEntity);
                        }
                        break;
                    }
                }
            }
        }
        return new Pair<>(entitiesSet, entitiesList);
    }

    @NullMarked
    public record ReachableItemEntity(Object entity, Set<Object> entitySet, List<Object> randomAccessEntityList) {
    }
}
