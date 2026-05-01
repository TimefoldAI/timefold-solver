package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.SetUndoableActionable;

import org.jspecify.annotations.NonNull;

final class ToSetQuadCollector<A, B, C, D, Mapped_>
        extends
        UndoableActionableQuadCollector<A, B, C, D, Mapped_, Set<Mapped_>, SetUndoableActionable.State<Mapped_>, SetUndoableActionable<Mapped_>> {
    ToSetQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
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
