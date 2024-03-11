package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class MapTriToTriNode<A, B, C, NewA, NewB, NewC>
        extends AbstractMapNode<TriTuple<A, B, C>, TriTuple<NewA, NewB, NewC>> {

    private final TriFunction<A, B, C, NewA> mappingFunctionA;
    private final TriFunction<A, B, C, NewB> mappingFunctionB;
    private final TriFunction<A, B, C, NewC> mappingFunctionC;

    MapTriToTriNode(int mapStoreIndex, TriFunction<A, B, C, NewA> mappingFunctionA, TriFunction<A, B, C, NewB> mappingFunctionB,
            TriFunction<A, B, C, NewC> mappingFunctionC, TupleLifecycle<TriTuple<NewA, NewB, NewC>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
    }

    @Override
    protected TriTuple<NewA, NewB, NewC> map(TriTuple<A, B, C> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        C factC = tuple.factC;
        return new TriTuple<>(
                mappingFunctionA.apply(factA, factB, factC),
                mappingFunctionB.apply(factA, factB, factC),
                mappingFunctionC.apply(factA, factB, factC),
                outputStoreSize);
    }

    @Override
    protected void remap(TriTuple<A, B, C> inTuple, TriTuple<NewA, NewB, NewC> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        C factC = inTuple.factC;
        NewA newA = mappingFunctionA.apply(factA, factB, factC);
        NewB newB = mappingFunctionB.apply(factA, factB, factC);
        NewC newC = mappingFunctionC.apply(factA, factB, factC);
        outTuple.factA = newA;
        outTuple.factB = newB;
        outTuple.factC = newC;
    }

}
