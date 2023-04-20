package ai.timefold.solver.constraint.streams.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractMapNode;
import ai.timefold.solver.constraint.streams.bavet.common.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.uni.UniTuple;
import ai.timefold.solver.core.api.function.TriFunction;

final class MapTriNode<A, B, C, NewA> extends AbstractMapNode<TriTuple<A, B, C>, NewA> {

    private final TriFunction<A, B, C, NewA> mappingFunction;

    MapTriNode(int mapStoreIndex, TriFunction<A, B, C, NewA> mappingFunction,
            TupleLifecycle<UniTuple<NewA>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    protected NewA map(TriTuple<A, B, C> tuple) {
        return mappingFunction.apply(tuple.getFactA(), tuple.getFactB(), tuple.getFactC());
    }

}
