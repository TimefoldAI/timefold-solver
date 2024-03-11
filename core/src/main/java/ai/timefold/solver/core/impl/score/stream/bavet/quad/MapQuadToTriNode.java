package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class MapQuadToTriNode<A, B, C, D, NewA, NewB, NewC>
        extends AbstractMapNode<QuadTuple<A, B, C, D>, TriTuple<NewA, NewB, NewC>> {

    private final QuadFunction<A, B, C, D, NewA> mappingFunctionA;
    private final QuadFunction<A, B, C, D, NewB> mappingFunctionB;
    private final QuadFunction<A, B, C, D, NewC> mappingFunctionC;

    MapQuadToTriNode(int mapStoreIndex, QuadFunction<A, B, C, D, NewA> mappingFunctionA,
            QuadFunction<A, B, C, D, NewB> mappingFunctionB, QuadFunction<A, B, C, D, NewC> mappingFunctionC,
            TupleLifecycle<TriTuple<NewA, NewB, NewC>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
    }

    @Override
    protected TriTuple<NewA, NewB, NewC> map(QuadTuple<A, B, C, D> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        C factC = tuple.factC;
        D factD = tuple.factD;
        return new TriTuple<>(
                mappingFunctionA.apply(factA, factB, factC, factD),
                mappingFunctionB.apply(factA, factB, factC, factD),
                mappingFunctionC.apply(factA, factB, factC, factD),
                outputStoreSize);
    }

    @Override
    protected void remap(QuadTuple<A, B, C, D> inTuple, TriTuple<NewA, NewB, NewC> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        C factC = inTuple.factC;
        D factD = inTuple.factD;
        NewA newA = mappingFunctionA.apply(factA, factB, factC, factD);
        NewB newB = mappingFunctionB.apply(factA, factB, factC, factD);
        NewC newC = mappingFunctionC.apply(factA, factB, factC, factD);
        outTuple.factA = newA;
        outTuple.factB = newB;
        outTuple.factC = newC;
    }

}
