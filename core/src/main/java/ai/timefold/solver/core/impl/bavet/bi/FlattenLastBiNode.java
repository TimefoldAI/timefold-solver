package ai.timefold.solver.core.impl.bavet.bi;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractFlattenNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class FlattenLastBiNode<A, B, NewB> extends AbstractFlattenNode<BiTuple<A, B>, BiTuple<A, NewB>, NewB> {

    private final Function<B, Iterable<NewB>> mappingFunction;
    private final int outputStoreSize;

    public FlattenLastBiNode(int flattenLastStoreIndex, Function<B, Iterable<NewB>> mappingFunction,
            TupleLifecycle<BiTuple<A, NewB>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(flattenLastStoreIndex, nextNodesTupleLifecycle);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected BiTuple<A, NewB> createTuple(BiTuple<A, B> originalTuple, NewB newB) {
        return BiTuple.of(originalTuple.getA(), newB, outputStoreSize);
    }

    @Override
    protected Iterable<NewB> extractIterable(BiTuple<A, B> tuple) {
        return mappingFunction.apply(tuple.getB());
    }

}
