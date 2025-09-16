package ai.timefold.solver.core.impl.bavet.bi;

import ai.timefold.solver.core.impl.bavet.common.AbstractFilterNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.function.BiPredicate;

@NullMarked
public final class FilterBiNode<A, B>
        extends AbstractFilterNode<BiTuple<A, B>> {

    private final BiPredicate<A, B> predicate;

    public FilterBiNode(int inputStoreIndex, BiPredicate<A, B> predicate,
                        TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> tuple) {
        return predicate.test(tuple.factA, tuple.factB);
    }

}
