package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.move.streams.maybeapi.UniDataFilter;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveStreamFactory<Solution_> {

    /**
     * Start a {@link DataStream} of all instances of the sourceClass
     * that are known as {@link ProblemFactCollectionProperty problem facts}
     * or {@link PlanningEntity planning entities}.
     * <p>
     * If the sourceClass is a {@link PlanningEntity}, then it is automatically
     * {@link UniDataStream#filter(UniDataFilter) filtered} to only contain entities
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
     * They can easily be {@link UniDataStream#filter(UniDataFilter) filtered out}.
     *
     * @return A stream containing a tuple for each of the entities as described above.
     * @see PlanningPin An annotation to mark the entire entity as pinned.
     * @see PlanningPinToIndex An annotation to specify only a portion of {@link PlanningListVariable} is pinned.
     * @see #forEachUnfiltered(Class, boolean) Specialized method exists to automatically include pinned entities as
     *      well.
     */
    <A> UniDataStream<Solution_, A> forEach(Class<A> sourceClass, boolean includeNull);

    /**
     * Start a {@link DataStream} of all instances of the sourceClass
     * that are known as {@link ProblemFactCollectionProperty problem facts}
     * or {@link PlanningEntity planning entities}.
     * If the sourceClass is a genuine or shadow entity,
     * it returns instances regardless of their pinning status.
     * Otherwise as defined by {@link #forEach(Class, boolean)}.
     */
    <A> UniDataStream<Solution_, A> forEachUnfiltered(Class<A> sourceClass, boolean includeNull);

    /**
     * Enumerate possible values for any given entity,
     * where entities are obtained using {@link #forEach(Class, boolean)},
     * with the class matching the entity type of the variable.
     * If the variable allows unassigned values, the resulting stream will include a null value.
     *
     * @param variableMetaModel the meta model of the variable to enumerate
     * @return data stream with all possible values of a given variable
     */
    default <Entity_, Value_> BiDataStream<Solution_, Entity_, Value_>
            forEachEntityValuePair(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return forEachEntityValuePair(variableMetaModel, forEach(variableMetaModel.entity().type(), false));
    }

    /**
     * Enumerate possible values for any given entity.
     * If the variable allows unassigned values, the resulting stream will include a null value.
     *
     * @param variableMetaModel the meta model of the variable to enumerate
     * @param entityDataStream the data stream of entities to enumerate values for
     * @return data stream with all possible values of a given variable
     */
    <Entity_, Value_> BiDataStream<Solution_, Entity_, Value_> forEachEntityValuePair(
            GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniDataStream<Solution_, Entity_> entityDataStream);

    <A> UniMoveStream<Solution_, A> pick(UniDataStream<Solution_, A> dataStream);

    <A, B> BiMoveStream<Solution_, A, B> pick(BiDataStream<Solution_, A, B> dataStream);

}
