package ai.timefold.solver.constraint.streams.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractMapNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.function.TriFunction;

final class MapTriToBiNode<A, B, C, NewA, NewB> extends AbstractMapNode<TriTuple<A, B, C>, BiTuple<NewA, NewB>> {

    private final TriFunction<A, B, C, NewA> mappingFunctionA;
    private final TriFunction<A, B, C, NewB> mappingFunctionB;

    MapTriToBiNode(int mapStoreIndex, TriFunction<A, B, C, NewA> mappingFunctionA, TriFunction<A, B, C, NewB> mappingFunctionB,
            TupleLifecycle<BiTuple<NewA, NewB>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
    }

    @Override
    protected BiTuple<NewA, NewB> map(TriTuple<A, B, C> tuple) {
        A factA = tuple.factA;
        B factB = tuple.factB;
        C factC = tuple.factC;
        return new BiTuple<>(
                mappingFunctionA.apply(factA, factB, factC),
                mappingFunctionB.apply(factA, factB, factC),
                outputStoreSize);
    }

    @Override
    protected boolean remap(TriTuple<A, B, C> inTuple, BiTuple<NewA, NewB> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        C factC = inTuple.factC;
        NewA newA = mappingFunctionA.apply(factA, factB, factC);
        NewB newB = mappingFunctionB.apply(factA, factB, factC);
        return outTuple.updateIfDifferent(newA, newB);
    }

}
