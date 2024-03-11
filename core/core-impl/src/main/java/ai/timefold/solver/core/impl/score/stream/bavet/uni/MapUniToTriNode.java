package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class MapUniToTriNode<A, NewA, NewB, NewC> extends AbstractMapNode<UniTuple<A>, TriTuple<NewA, NewB, NewC>> {

    private final Function<A, NewA> mappingFunctionA;
    private final Function<A, NewB> mappingFunctionB;
    private final Function<A, NewC> mappingFunctionC;

    MapUniToTriNode(int mapStoreIndex, Function<A, NewA> mappingFunctionA, Function<A, NewB> mappingFunctionB,
            Function<A, NewC> mappingFunctionC, TupleLifecycle<TriTuple<NewA, NewB, NewC>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
    }

    @Override
    protected TriTuple<NewA, NewB, NewC> map(UniTuple<A> tuple) {
        A factA = tuple.factA;
        return new TriTuple<>(
                mappingFunctionA.apply(factA),
                mappingFunctionB.apply(factA),
                mappingFunctionC.apply(factA),
                outputStoreSize);
    }

    @Override
    protected void remap(UniTuple<A> inTuple, TriTuple<NewA, NewB, NewC> outTuple) {
        A factA = inTuple.factA;
        NewA newA = mappingFunctionA.apply(factA);
        NewB newB = mappingFunctionB.apply(factA);
        NewC newC = mappingFunctionC.apply(factA);
        outTuple.factA = newA;
        outTuple.factB = newB;
        outTuple.factC = newC;
    }

}
