package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

public interface MoveStreams<Solution_> {

    <A> CachedMoveUniStream<Solution_, A> enumerate(Class<A> clz);

    default <Entity_> CachedMoveUniStream<Solution_, Entity_>
            enumerateEntities(PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        return enumerate(entityMetaModel.type());
    }

    @SuppressWarnings("unchecked")
    default <Entity_, A> CachedMoveUniStream<Solution_, A>
            enumeratePossibleValues(PlanningVariableMetaModel<Solution_, Entity_, A> variableMetaModel) {
        var variableDescriptor =
                ((DefaultPlanningVariableMetaModel<Solution_, Entity_, A>) variableMetaModel).variableDescriptor();
        var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
        if (variableDescriptor.isValueRangeEntityIndependent()) {
            return enumerate(solution -> (Collection<A>) valueRangeDescriptor.extractValueRange(solution, null));
        } else {
            return enumerateFromEntity(variableMetaModel.entity(),
                    (solution, entity) -> (Collection<A>) valueRangeDescriptor.extractValueRange(solution, entity));
        }
    }

    <A> CachedMoveUniStream<Solution_, A> enumerate(Function<Solution_, Collection<A>> collectionFunction);

    <Entity_, A> CachedMoveUniStream<Solution_, A> enumerateFromEntity(
            PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel,
            BiFunction<Solution_, Entity_, Collection<A>> collectionFunction);

    <A> CachedMoveUniStream<Solution_, A> enumerate(Collection<A> collection);

    default <A> JitMoveUniStream<Solution_, A> pick(Class<A> clz) {
        return pick(enumerate(clz));
    }

    <A> JitMoveUniStream<Solution_, A> pick(CachedMoveUniStream<Solution_, A> cachedMoveUniStream);

}
