package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.CustomCollectionUndoableActionable;

public final class CustomCollectionBiCollector<A, B, Mapped, Result extends Collection<Mapped>>
        extends UndoableActionableBiCollector<A, B, Mapped, Result, CustomCollectionUndoableActionable<Mapped, Result>> {
    private final IntFunction<Result> collectionFunction;

    public CustomCollectionBiCollector(BiFunction<? super A, ? super B, ? extends Mapped> mapper,
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
        CustomCollectionBiCollector<?, ?, ?, ?> that = (CustomCollectionBiCollector<?, ?, ?, ?>) object;
        return Objects.equals(collectionFunction, that.collectionFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collectionFunction);
    }
}
