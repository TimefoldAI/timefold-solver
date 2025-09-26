package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ForEachIncludingPinnedEnumeratingStream<Solution_, A>
        extends AbstractForEachEnumeratingStream<Solution_, A>
        implements TupleSource {

    public ForEachIncludingPinnedEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            Class<A> forEachClass,
            boolean includeNull) {
        super(enumeratingStreamFactory, forEachClass, includeNull);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForEachIncludingPinnedEnumeratingStream<?, ?> that &&
                Objects.equals(shouldIncludeNull, that.shouldIncludeNull) &&
                Objects.equals(forEachClass, that.forEachClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shouldIncludeNull, forEachClass);
    }

    @Override
    public String toString() {
        return "ForEach (" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
    }

}
