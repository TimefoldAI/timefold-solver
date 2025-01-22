package ai.timefold.solver.core.impl.bavet.uni.collector;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.collector.SetUndoableActionable;

import org.jspecify.annotations.NonNull;

final class ToSetUniCollector<A, Mapped_>
        extends UndoableActionableUniCollector<A, Mapped_, Set<Mapped_>, SetUndoableActionable<Mapped_>> {
    ToSetUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<SetUndoableActionable<Mapped_>> supplier() {
        return SetUndoableActionable::new;
    }
}
