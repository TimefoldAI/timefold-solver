package ai.timefold.solver.core.api.domain.metamodel;

import java.util.List;

public interface SolutionMetaModel<Solution_> {

    Class<Solution_> type();

    List<EntityMetaModel<Solution_, ?>> entities();

    default List<EntityMetaModel<Solution_, ?>> genuineEntities() {
        return entities().stream()
                .filter(EntityMetaModel::isGenuine)
                .toList();
    }

    @SuppressWarnings("unchecked")
    default <Entity_> EntityMetaModel<Solution_, Entity_> entity(Class<Entity_> entityClass) {
        for (var entityMetaModel : entities()) {
            if (entityMetaModel.type().equals(entityClass)) {
                return (EntityMetaModel<Solution_, Entity_>) entityMetaModel;
            }
        }
        throw new IllegalArgumentException(
                "The type (" + entityClass + ") does not exist in the entities (" + entities() + ").");
    }

    default boolean hasListVariable() {
        return entities().stream()
                .filter(EntityMetaModel::isGenuine)
                .flatMap(entityMetaModel -> entityMetaModel.variables().stream())
                .anyMatch(VariableMetaModel::isList);
    }

}
