package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class MapBiToTriNode<A, B, NewA, NewB, NewC> extends AbstractMapNode<BiTuple<A, B>, TriTuple<NewA, NewB, NewC>> {

    private final BiFunction<A, B, NewA> mappingFunctionA;
    private final BiFunction<A, B, NewB> mappingFunctionB;
    private final BiFunction<A, B, NewC> mappingFunctionC;

    MapBiToTriNode(int mapStoreIndex, BiFunction<A, B, NewA> mappingFunctionA, BiFunction<A, B, NewB> mappingFunctionB,
            BiFunction<A, B, NewC> mappingFunctionC,
            TupleLifecycle<TriTuple<NewA, NewB, NewC>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
    }

    @Override
    protected TriTuple<NewA, NewB, NewC> map(BiTuple<A, B> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        return new TriTuple<>(
                mappingFunctionA.apply(factA, factB),
                mappingFunctionB.apply(factA, factB),
                mappingFunctionC.apply(factA, factB),
                outputStoreSize);
    }

    @Override
    protected void remap(BiTuple<A, B> inTuple, TriTuple<NewA, NewB, NewC> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        NewA newA = mappingFunctionA.apply(factA, factB);
        NewB newB = mappingFunctionB.apply(factA, factB);
        NewC newC = mappingFunctionC.apply(factA, factB);
        outTuple.factA = newA;
        outTuple.factB = newB;
        outTuple.factC = newC;
    }

}
