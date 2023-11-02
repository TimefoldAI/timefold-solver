package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.Set;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.SetUndoableActionable;

public final class SetQuadCollector<A, B, C, D, Mapped_>
        extends UndoableActionableQuadCollector<A, B, C, D, Mapped_, Set<Mapped_>, SetUndoableActionable<Mapped_>> {
    public SetQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<SetUndoableActionable<Mapped_>> supplier() {
        return SetUndoableActionable::new;
    }
}
