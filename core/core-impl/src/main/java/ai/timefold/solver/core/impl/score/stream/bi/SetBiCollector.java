package ai.timefold.solver.core.impl.score.stream.bi;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.SetUndoableActionable;

public final class SetBiCollector<A, B, Mapped_>
        extends UndoableActionableBiCollector<A, B, Mapped_, Set<Mapped_>, SetUndoableActionable<Mapped_>> {
    public SetBiCollector(BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        super(mapper);
    }

    @Override
    public Supplier<SetUndoableActionable<Mapped_>> supplier() {
        return SetUndoableActionable::new;
    }
}
