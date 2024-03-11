package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class MapBiToBiNode<A, B, NewA, NewB> extends AbstractMapNode<BiTuple<A, B>, BiTuple<NewA, NewB>> {

    private final BiFunction<A, B, NewA> mappingFunctionA;
    private final BiFunction<A, B, NewB> mappingFunctionB;

    MapBiToBiNode(int mapStoreIndex, BiFunction<A, B, NewA> mappingFunctionA, BiFunction<A, B, NewB> mappingFunctionB,
            TupleLifecycle<BiTuple<NewA, NewB>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
    }

    @Override
    protected BiTuple<NewA, NewB> map(BiTuple<A, B> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        return new BiTuple<>(
                mappingFunctionA.apply(factA, factB),
                mappingFunctionB.apply(factA, factB),
                outputStoreSize);
    }

    @Override
    protected void remap(BiTuple<A, B> inTuple, BiTuple<NewA, NewB> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        NewA newA = mappingFunctionA.apply(factA, factB);
        NewB newB = mappingFunctionB.apply(factA, factB);
        outTuple.factA = newA;
        outTuple.factB = newB;
    }

}
