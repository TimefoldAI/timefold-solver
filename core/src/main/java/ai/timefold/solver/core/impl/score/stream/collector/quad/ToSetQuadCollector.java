package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Set;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.SetUndoableActionable;

final class ToSetQuadCollector<A, B, C, D, Mapped_>
        extends UndoableActionableQuadCollector<A, B, C, D, Mapped_, Set<Mapped_>, SetUndoableActionable<Mapped_>> {
    ToSetQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<SetUndoableActionable<Mapped_>> supplier() {
        return SetUndoableActionable::new;
    }
}
