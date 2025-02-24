package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Collection;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class ForEachStaticUniNode<A>
        extends AbstractForEachUniNode<A> {

    private final Collection<A> source;

    public ForEachStaticUniNode(Class<A> forEachClass, Collection<A> source,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
        this.source = Objects.requireNonNull(source);
    }

    public void initialize() {
        source.forEach(super::insert);
    }

    @Override
    public void insert(A a) {
        throw new IllegalStateException("Impossible state: static node cannot insert.");
    }

    @Override
    public void update(A a) {
        throw new IllegalStateException("Impossible state: static node cannot update.");
    }

    @Override
    public void retract(A a) {
        throw new IllegalStateException("Impossible state: static node cannot retract.");
    }

    @Override
    public boolean supportsIndividualUpdates() {
        return false;
    }

}
