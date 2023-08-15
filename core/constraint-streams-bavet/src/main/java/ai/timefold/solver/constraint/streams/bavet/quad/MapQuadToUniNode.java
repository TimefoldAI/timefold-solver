package ai.timefold.solver.constraint.streams.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractMapNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.api.function.QuadFunction;

final class MapQuadToUniNode<A, B, C, D, NewA> extends AbstractMapNode<QuadTuple<A, B, C, D>, UniTuple<NewA>> {

    private final QuadFunction<A, B, C, D, NewA> mappingFunction;

    MapQuadToUniNode(int mapStoreIndex, QuadFunction<A, B, C, D, NewA> mappingFunction,
            TupleLifecycle<UniTuple<NewA>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    protected UniTuple<NewA> map(QuadTuple<A, B, C, D> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        C factC = tuple.factC;
        D factD = tuple.factD;
        return new UniTuple<>(
                mappingFunction.apply(factA, factB, factC, factD),
                outputStoreSize);
    }

    @Override
    protected boolean remap(QuadTuple<A, B, C, D> inTuple, UniTuple<NewA> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        C factC = inTuple.factC;
        D factD = inTuple.factD;
        NewA newA = mappingFunction.apply(factA, factB, factC, factD);
        return outTuple.updateIfDifferent(newA);
    }

}
