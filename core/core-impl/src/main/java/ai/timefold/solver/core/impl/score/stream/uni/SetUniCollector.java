package ai.timefold.solver.core.impl.score.stream.uni;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.SetUndoableActionable;

public final class SetUniCollector<A, Mapped_>
        extends UndoableActionableUniCollector<A, Mapped_, Set<Mapped_>, SetUndoableActionable<Mapped_>> {
    public SetUniCollector(Function<? super A, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<SetUndoableActionable<Mapped_>> supplier() {
        return SetUndoableActionable::new;
    }
}
