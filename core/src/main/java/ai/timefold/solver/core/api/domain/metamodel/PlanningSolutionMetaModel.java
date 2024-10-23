package ai.timefold.solver.core.api.domain.metamodel;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

/**
 * Represents the meta-model of a {@link PlanningSolution}.
 * Allows access to information about all the entities and variables.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 *
 * @param <Solution_> the type of the solution
 */
public interface PlanningSolutionMetaModel<Solution_> {

    /**
     * Returns the class of the solution.
     * 
     * @return never null, the type of the solution
     */
    Class<Solution_> type();

    /**
     * Returns the meta-models of @{@link PlanningEntity planning entities} known to the solution, genuine or shadow.
     * 
     * @return never null
     */
    List<PlanningEntityMetaModel<Solution_, ?>> entities();

    /**
     * Returns the meta-models of genuine @{@link PlanningEntity planning entities} known to the solution.
     *
     * @return never null
     */
    default List<PlanningEntityMetaModel<Solution_, ?>> genuineEntities() {
        return entities().stream()
                .filter(PlanningEntityMetaModel::isGenuine)
                .toList();
    }

    /**
     * Returns the meta-model of the @{@link PlanningEntity planning entity} with the given class.
     *
     * @param entityClass never null
     * @return never null
     * @throws IllegalArgumentException if the entity class is not known to the solution
     */
    @SuppressWarnings("unchecked")
    default <Entity_> PlanningEntityMetaModel<Solution_, Entity_> entity(Class<Entity_> entityClass) {
        for (var entityMetaModel : entities()) {
            if (entityMetaModel.type().equals(entityClass)) {
                return (PlanningEntityMetaModel<Solution_, Entity_>) entityMetaModel;
            }
        }
        throw new IllegalArgumentException(
                "The type (%s) does not exist in the entities (%s).".formatted(entityClass, entities()));
    }

}
