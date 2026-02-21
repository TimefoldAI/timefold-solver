package ai.timefold.solver.core.preview.api.neighborhood.stream;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.EnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.UniSamplingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveStreamFactory<Solution_> {

    /**
     * Start a {@link EnumeratingStream} of all instances of the sourceClass
     * that are known as {@link ProblemFactCollectionProperty problem facts}
     * or {@link PlanningEntity planning entities}.
     * <p>
     * If the sourceClass is a {@link PlanningEntity}, then it is automatically
     * {@link UniEnumeratingStream#filter(UniNeighborhoodsPredicate) filtered} to only contain entities
     * which are not pinned.
     * <p>
     * If the sourceClass is a shadow entity (an entity without any genuine planning variables),
     * and if there exists a genuine {@link PlanningEntity} with a {@link PlanningListVariable}
     * which accepts instances of this shadow entity as values in that list,
     * then this stream will filter out all sourceClass instances
     * which are pinned in that list.
     * <p>
     * This stream returns genuine entities regardless of whether they have any null genuine planning variables.
     * This stream returns shadow entities regardless of whether they are assigned to any genuine entity.
     * They can easily be {@link UniEnumeratingStream#filter(UniNeighborhoodsPredicate) filtered out}.
     *
     * @param sourceClass the class of the instances to enumerate
     * @param includeNull if true, the stream will include a single null element
     * @return A stream containing a tuple for each of the entities as described above.
     * @see PlanningPin An annotation to mark the entire entity as pinned.
     * @see PlanningPinToIndex An annotation to specify only a portion of {@link PlanningListVariable} is pinned.
     * @see #forEachUnfiltered(Class, boolean) Specialized method exists to automatically include pinned entities as
     *      well.
     */
    <A> UniEnumeratingStream<Solution_, A> forEach(Class<A> sourceClass, boolean includeNull);

    /**
     * Start a {@link EnumeratingStream} of all instances of the sourceClass
     * that are known as {@link ProblemFactCollectionProperty problem facts}
     * or {@link PlanningEntity planning entities}.
     * If the sourceClass is a genuine or shadow entity,
     * it returns instances regardless of their pinning status.
     * Otherwise as defined by {@link #forEach(Class, boolean)}.
     */
    <A> UniEnumeratingStream<Solution_, A> forEachUnfiltered(Class<A> sourceClass, boolean includeNull);

    /**
     * Enumerate all values assigned to any entity's {@link PlanningListVariable}.
     * Unlike {@link #forEachAssignedValue(PlanningListVariableMetaModel)}, this will include pinned values.
     * You can use {@link SolutionView#getPositionOf(PlanningListVariableMetaModel, Object)}
     * later downstream to get the position of the value in an entity's list variable, if needed.
     *
     * @param variableMetaModel the meta model of the list variable to enumerate
     * @return enumerating stream with all values as defined above
     * @see PlanningPin An annotation to mark the entire entity as pinned.
     * @see PlanningPinToIndex An annotation to specify only a portion of {@link PlanningListVariable} is pinned.
     */
    <Entity_, Value_> UniEnumeratingStream<Solution_, Value_>
            forEachAssignedValueUnfiltered(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel);

    /**
     * Enumerate all values assigned to any entity's {@link PlanningListVariable}.
     * This will not include any pinned positions or fully pinned entities.
     * You can use {@link SolutionView#getPositionOf(PlanningListVariableMetaModel, Object)}
     * later downstream to get the position of the value in an entity's list variable, if needed.
     *
     * @param variableMetaModel the meta model of the list variable to enumerate
     * @return enumerating stream with all values as defined above
     * @see PlanningPin An annotation to mark the entire entity as pinned.
     * @see PlanningPinToIndex An annotation to specify only a portion of {@link PlanningListVariable} is pinned.
     */
    <Entity_, Value_> UniEnumeratingStream<Solution_, Value_>
            forEachAssignedValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel);

    /**
     * Enumerate all possible positions of a list variable to which a value can be assigned.
     * This will include one position past the current end of the list, allowing for assigning at the end of a list.
     * It will not include any pinned positions or fully pinned entities, as well as {@link UnassignedElement}.
     * To include {@link UnassignedElement},
     * use {@link #forEachDestinationIncludingUnassigned(PlanningListVariableMetaModel)} instead.
     *
     * @param variableMetaModel the meta model of the list variable to enumerate
     * @return enumerating stream with positions as defined above
     * @see ElementPosition Read more about element positions.
     * @see PlanningPin An annotation to mark the entire entity as pinned.
     * @see PlanningPinToIndex An annotation to specify only a portion of {@link PlanningListVariable} is pinned.
     */
    <Entity_, Value_> UniEnumeratingStream<Solution_, PositionInList>
            forEachDestination(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel);

    /**
     * As defined by {@link #forEachDestination(PlanningListVariableMetaModel)},
     * but also includes a single {@link UnassignedElement} position
     * if the list variable allows unassigned values.
     * If the list variable does not allow unassigned values,
     * then this method behaves exactly the same as {@link #forEachDestination(PlanningListVariableMetaModel)}.
     */
    <Entity_, Value_> UniEnumeratingStream<Solution_, ElementPosition>
            forEachDestinationIncludingUnassigned(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel);

    <A> UniSamplingStream<Solution_, A> pick(UniEnumeratingStream<Solution_, A> enumeratingStream);

}
