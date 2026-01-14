package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractFlattenLastNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class FlattenUniNode<A, NewB> extends AbstractFlattenLastNode<UniTuple<A>, BiTuple<A, NewB>, NewB> {

    private final Function<A, Iterable<NewB>> mappingFunction;
    private final int outputStoreSize;

    public FlattenUniNode(int flattenLastStoreIndex, Function<A, Iterable<NewB>> mappingFunction,
            TupleLifecycle<BiTuple<A, NewB>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(flattenLastStoreIndex, nextNodesTupleLifecycle);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected BiTuple<A, NewB> createTuple(UniTuple<A> originalTuple, NewB newB) {
        return BiTuple.of(originalTuple.getA(), newB, outputStoreSize);
    }

    @Override
    protected Iterable<NewB> extractIterable(UniTuple<A> tuple) {
        return mappingFunction.apply(tuple.getA());
    }

}
