package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class MapBiToQuadNode<A, B, NewA, NewB, NewC, NewD>
        extends AbstractMapNode<BiTuple<A, B>, QuadTuple<NewA, NewB, NewC, NewD>> {

    private final BiFunction<A, B, NewA> mappingFunctionA;
    private final BiFunction<A, B, NewB> mappingFunctionB;
    private final BiFunction<A, B, NewC> mappingFunctionC;
    private final BiFunction<A, B, NewD> mappingFunctionD;

    MapBiToQuadNode(int mapStoreIndex, BiFunction<A, B, NewA> mappingFunctionA, BiFunction<A, B, NewB> mappingFunctionB,
            BiFunction<A, B, NewC> mappingFunctionC, BiFunction<A, B, NewD> mappingFunctionD,
            TupleLifecycle<QuadTuple<NewA, NewB, NewC, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
        this.mappingFunctionD = Objects.requireNonNull(mappingFunctionD);
    }

    @Override
    protected QuadTuple<NewA, NewB, NewC, NewD> map(BiTuple<A, B> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        return new QuadTuple<>(
                mappingFunctionA.apply(factA, factB),
                mappingFunctionB.apply(factA, factB),
                mappingFunctionC.apply(factA, factB),
                mappingFunctionD.apply(factA, factB),
                outputStoreSize);
    }

    @Override
    protected void remap(BiTuple<A, B> inTuple, QuadTuple<NewA, NewB, NewC, NewD> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        NewA newA = mappingFunctionA.apply(factA, factB);
        NewB newB = mappingFunctionB.apply(factA, factB);
        NewC newC = mappingFunctionC.apply(factA, factB);
        NewD newD = mappingFunctionD.apply(factA, factB);
        outTuple.factA = newA;
        outTuple.factB = newB;
        outTuple.factC = newC;
        outTuple.factD = newD;
    }

}
