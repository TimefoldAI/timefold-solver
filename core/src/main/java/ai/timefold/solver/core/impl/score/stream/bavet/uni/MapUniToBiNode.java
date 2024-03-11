package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class MapUniToBiNode<A, NewA, NewB> extends AbstractMapNode<UniTuple<A>, BiTuple<NewA, NewB>> {

    private final Function<A, NewA> mappingFunctionA;
    private final Function<A, NewB> mappingFunctionB;

    MapUniToBiNode(int mapStoreIndex, Function<A, NewA> mappingFunctionA, Function<A, NewB> mappingFunctionB,
            TupleLifecycle<BiTuple<NewA, NewB>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
    }

    @Override
    protected BiTuple<NewA, NewB> map(UniTuple<A> tuple) {
        A factA = tuple.factA;
        return new BiTuple<>(
                mappingFunctionA.apply(factA),
                mappingFunctionB.apply(factA),
                outputStoreSize);
    }

    @Override
    protected void remap(UniTuple<A> inTuple, BiTuple<NewA, NewB> outTuple) {
        A factA = inTuple.factA;
        NewA newA = mappingFunctionA.apply(factA);
        NewB newB = mappingFunctionB.apply(factA);
        outTuple.factA = newA;
        outTuple.factB = newB;
    }

}
