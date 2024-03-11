package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class MapQuadToQuadNode<A, B, C, D, NewA, NewB, NewC, NewD>
        extends AbstractMapNode<QuadTuple<A, B, C, D>, QuadTuple<NewA, NewB, NewC, NewD>> {

    private final QuadFunction<A, B, C, D, NewA> mappingFunctionA;
    private final QuadFunction<A, B, C, D, NewB> mappingFunctionB;
    private final QuadFunction<A, B, C, D, NewC> mappingFunctionC;
    private final QuadFunction<A, B, C, D, NewD> mappingFunctionD;

    MapQuadToQuadNode(int mapStoreIndex, QuadFunction<A, B, C, D, NewA> mappingFunctionA,
            QuadFunction<A, B, C, D, NewB> mappingFunctionB, QuadFunction<A, B, C, D, NewC> mappingFunctionC,
            QuadFunction<A, B, C, D, NewD> mappingFunctionD,
            TupleLifecycle<QuadTuple<NewA, NewB, NewC, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
        this.mappingFunctionD = Objects.requireNonNull(mappingFunctionD);
    }

    @Override
    protected QuadTuple<NewA, NewB, NewC, NewD> map(QuadTuple<A, B, C, D> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        C factC = tuple.factC;
        D factD = tuple.factD;
        return new QuadTuple<>(
                mappingFunctionA.apply(factA, factB, factC, factD),
                mappingFunctionB.apply(factA, factB, factC, factD),
                mappingFunctionC.apply(factA, factB, factC, factD),
                mappingFunctionD.apply(factA, factB, factC, factD),
                outputStoreSize);
    }

    @Override
    protected void remap(QuadTuple<A, B, C, D> inTuple, QuadTuple<NewA, NewB, NewC, NewD> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        C factC = inTuple.factC;
        D factD = inTuple.factD;
        NewA newA = mappingFunctionA.apply(factA, factB, factC, factD);
        NewB newB = mappingFunctionB.apply(factA, factB, factC, factD);
        NewC newC = mappingFunctionC.apply(factA, factB, factC, factD);
        NewD newD = mappingFunctionD.apply(factA, factB, factC, factD);
        outTuple.factA = newA;
        outTuple.factB = newB;
        outTuple.factC = newC;
        outTuple.factD = newD;
    }

}
