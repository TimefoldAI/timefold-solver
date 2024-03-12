package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractFlattenLastNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class FlattenLastUniNode<A, NewA> extends AbstractFlattenLastNode<UniTuple<A>, UniTuple<NewA>, A, NewA> {

    private final int outputStoreSize;

    FlattenLastUniNode(int flattenLastStoreIndex, Function<A, Iterable<NewA>> mappingFunction,
            TupleLifecycle<UniTuple<NewA>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(flattenLastStoreIndex, mappingFunction, nextNodesTupleLifecycle);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected UniTuple<NewA> createTuple(UniTuple<A> originalTuple, NewA item) {
        return new UniTuple<>(item, outputStoreSize);
    }

    @Override
    protected A getEffectiveFactIn(UniTuple<A> tuple) {
        return tuple.factA;
    }

    @Override
    protected NewA getEffectiveFactOut(UniTuple<NewA> outTuple) {
        return outTuple.factA;
    }

}
