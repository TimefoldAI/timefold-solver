package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractFilterNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class FilterUniNode<A>
        extends AbstractFilterNode<UniTuple<A>> {

    private final Predicate<A> predicate;

    public FilterUniNode(int inputStoreIndex, Predicate<A> predicate,
                         TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle) {
        super(inputStoreIndex, nextNodesTupleLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
    }

    @Override
    protected boolean testFiltering(UniTuple<A> tuple) {
        return predicate.test(tuple.factA);
    }

}
