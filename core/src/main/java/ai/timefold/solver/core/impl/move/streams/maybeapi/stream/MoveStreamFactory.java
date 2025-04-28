package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import java.util.function.Predicate;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveStreamFactory<Solution_> {

    /**
     * Start a {@link ConstraintStream} of all instances of the sourceClass
     * that are known as {@link ProblemFactCollectionProperty problem facts} or {@link PlanningEntity planning entities}.
     * <p>
     * If the sourceClass is a {@link PlanningEntity}, then it is automatically
     * {@link UniDataStream#filter(Predicate) filtered} to only contain entities
     * which are pinned.
     * <p>
     * If the sourceClass is a shadow entity (an entity without any genuine planning variables),
     * and if there exists a genuine {@link PlanningEntity} with a {@link PlanningListVariable}
     * which accepts instances of this shadow entity as values in that list,
     * then this stream will filter out all sourceClass instances
     * which are pinned in that list.
     * <p>
     * This stream returns genuine entities regardless of whether they have any null genuine planning variables.
     * This stream returns shadow entities regardless of whether they are assigned to any genuine entity.
     * They can easily be {@link UniDataStream#filter(Predicate) filtered out}.
     *
     * @return A stream containing a tuple for each of the entities as described above.
     * @see PlanningPin An annotation to mark the entire entity as pinned.
     * @see PlanningPinToIndex An annotation to specify only a portion of {@link PlanningListVariable} is pinned.
     * @see #enumerateIncludingPinned(Class) Specialized method exists to automatically include pinned entities as well.
     */
    <A> UniDataStream<Solution_, A> enumerate(Class<A> sourceClass);

    /**
     * Start a {@link ConstraintStream} of all instances of the sourceClass
     * that are known as {@link ProblemFactCollectionProperty problem facts} or {@link PlanningEntity planning entities}.
     * If the sourceClass is a genuine or shadow entity,
     * it returns instances regardless of their pinning status.
     * Otherwise as defined by {@link #enumerate(Class)}.
     */
    <A> UniDataStream<Solution_, A> enumerateIncludingPinned(Class<A> sourceClass);

    /**
     * Enumerate possible values for a given basic variable.
     * If the variable allows unassigned values, the resulting stream will include a null value.
     *
     * @throws UnsupportedOperationException If the variable in question is a list variable,
     *         or if the basic variable is chained.
     * @return data stream with all possible values of a given variable
     * @see #enumeratePossiblePositions(PlanningListVariableMetaModel) For list variables, use a specialized method.
     */
    <Entity_, A> UniDataStream<Solution_, A>
            enumeratePossibleValues(PlanningVariableMetaModel<Solution_, Entity_, A> variableMetaModel);

    default <Entity_, A> UniDataStream<Solution_, A>
            enumeratePossiblePositions(PlanningListVariableMetaModel<Solution_, Entity_, A> variableMetaModel) {
        throw new UnsupportedOperationException(); // TODO
    }

    default <A> UniMoveStream<Solution_, A> pick(Class<A> clz) {
        return pick(enumerate(clz));
    }

    <A> UniMoveStream<Solution_, A> pick(UniDataStream<Solution_, A> dataStream);

}
