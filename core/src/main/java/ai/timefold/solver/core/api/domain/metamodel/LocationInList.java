package ai.timefold.solver.core.api.domain.metamodel;

/**
 * Uniquely identifies the location of a value in a list variable.
 * Within that one list, the index is unique for each value and therefore the instances are comparable.
 * Comparing them between different lists has no meaning.
 */
public sealed interface LocationInList<Entity_>
        extends ElementLocation, Comparable<LocationInList<Entity_>>
        permits DefaultLocationInList {

    Entity_ entity();

    int index();

}
