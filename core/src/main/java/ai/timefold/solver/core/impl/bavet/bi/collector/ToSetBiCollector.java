package ai.timefold.solver.core.impl.bavet.bi.collector;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.collector.SetUndoableActionable;

import org.jspecify.annotations.NonNull;

final class ToSetBiCollector<A, B, Mapped_>
        extends UndoableActionableBiCollector<A, B, Mapped_, Set<Mapped_>, SetUndoableActionable<Mapped_>> {
    ToSetBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<SetUndoableActionable<Mapped_>> supplier() {
        return SetUndoableActionable::new;
    }
}
