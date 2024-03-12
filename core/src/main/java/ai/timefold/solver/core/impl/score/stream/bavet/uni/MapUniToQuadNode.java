package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class MapUniToQuadNode<A, NewA, NewB, NewC, NewD>
        extends AbstractMapNode<UniTuple<A>, QuadTuple<NewA, NewB, NewC, NewD>> {

    private final Function<A, NewA> mappingFunctionA;
    private final Function<A, NewB> mappingFunctionB;
    private final Function<A, NewC> mappingFunctionC;
    private final Function<A, NewD> mappingFunctionD;

    MapUniToQuadNode(int mapStoreIndex, Function<A, NewA> mappingFunctionA, Function<A, NewB> mappingFunctionB,
            Function<A, NewC> mappingFunctionC, Function<A, NewD> mappingFunctionD,
            TupleLifecycle<QuadTuple<NewA, NewB, NewC, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
        this.mappingFunctionD = Objects.requireNonNull(mappingFunctionD);
    }

    @Override
    protected QuadTuple<NewA, NewB, NewC, NewD> map(UniTuple<A> tuple) {
        A factA = tuple.factA;
        return new QuadTuple<>(
                mappingFunctionA.apply(factA),
                mappingFunctionB.apply(factA),
                mappingFunctionC.apply(factA),
                mappingFunctionD.apply(factA),
                outputStoreSize);
    }

    @Override
    protected void remap(UniTuple<A> inTuple, QuadTuple<NewA, NewB, NewC, NewD> outTuple) {
        A factA = inTuple.factA;
        NewA newA = mappingFunctionA.apply(factA);
        NewB newB = mappingFunctionB.apply(factA);
        NewC newC = mappingFunctionC.apply(factA);
        NewD newD = mappingFunctionD.apply(factA);
        outTuple.factA = newA;
        outTuple.factB = newB;
        outTuple.factC = newC;
        outTuple.factD = newD;
    }

}
