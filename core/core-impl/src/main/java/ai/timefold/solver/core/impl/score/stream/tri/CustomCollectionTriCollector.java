package ai.timefold.solver.core.impl.score.stream.tri;

import java.util.Collection;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.CustomCollectionUndoableActionable;

public final class CustomCollectionTriCollector<A, B, C, Mapped, Result extends Collection<Mapped>>
        extends
        UndoableActionableTriCollector<A, B, C, Mapped, Result, CustomCollectionUndoableActionable<Mapped, Result>> {
    private final IntFunction<Result> collectionFunction;

    public CustomCollectionTriCollector(TriFunction<? super A, ? super B, ? super C, ? extends Mapped> mapper,
            IntFunction<Result> collectionFunction) {
        super(mapper);
        this.collectionFunction = collectionFunction;
    }

    @Override
    public Supplier<CustomCollectionUndoableActionable<Mapped, Result>> supplier() {
        return () -> new CustomCollectionUndoableActionable<>(collectionFunction);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        CustomCollectionTriCollector<?, ?, ?, ?, ?> that = (CustomCollectionTriCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(collectionFunction, that.collectionFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collectionFunction);
    }
}
