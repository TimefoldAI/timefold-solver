package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.SetUndoableActionable;

import org.jspecify.annotations.NonNull;

final class ToSetBiCollector<A, B, Mapped_>
        extends
        UndoableActionableBiCollector<A, B, Mapped_, Set<Mapped_>, SetUndoableActionable.State<Mapped_>, SetUndoableActionable<Mapped_>> {
    ToSetBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public @NonNull Supplier<SetUndoableActionable.State<Mapped_>> supplier() {
        return SetUndoableActionable.State::new;
    }

    @Override
    public @NonNull Function<SetUndoableActionable.State<Mapped_>, Set<Mapped_>> finisher() {
        return SetUndoableActionable.State::result;
    }

    @Override
    protected SetUndoableActionable<Mapped_> newUndoableActionable(SetUndoableActionable.State<Mapped_> state) {
        return new SetUndoableActionable<>(state);
    }
}
