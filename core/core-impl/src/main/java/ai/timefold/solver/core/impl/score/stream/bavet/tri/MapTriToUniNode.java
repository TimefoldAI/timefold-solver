package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class MapTriToUniNode<A, B, C, NewA> extends AbstractMapNode<TriTuple<A, B, C>, UniTuple<NewA>> {

    private final TriFunction<A, B, C, NewA> mappingFunction;

    MapTriToUniNode(int mapStoreIndex, TriFunction<A, B, C, NewA> mappingFunction,
            TupleLifecycle<UniTuple<NewA>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    protected UniTuple<NewA> map(TriTuple<A, B, C> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        C factC = tuple.factC;
        return new UniTuple<>(
                mappingFunction.apply(factA, factB, factC),
                outputStoreSize);
    }

    @Override
    protected void remap(TriTuple<A, B, C> inTuple, UniTuple<NewA> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        C factC = inTuple.factC;
        NewA newA = mappingFunction.apply(factA, factB, factC);
        outTuple.factA = newA;
    }

}
