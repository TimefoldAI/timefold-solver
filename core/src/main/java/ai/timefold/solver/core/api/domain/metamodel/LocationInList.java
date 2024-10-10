package ai.timefold.solver.core.api.domain.metamodel;

/**
 * Points to a list variable position specified by an entity and an index.
 */
public sealed interface LocationInList<Entity_>
        extends ElementLocation
        permits DefaultLocationInList {

    Entity_ entity();

    int index();

}
