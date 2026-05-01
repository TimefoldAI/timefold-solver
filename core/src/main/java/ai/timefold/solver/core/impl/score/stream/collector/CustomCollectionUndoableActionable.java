package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;

public final class CustomCollectionUndoableActionable<Mapped_, Result_ extends Collection<Mapped_>>
        implements UndoableActionable<Mapped_> {
    public static final class State<Mapped_, Collection_ extends Collection<Mapped_>> {
        private final IntFunction<Collection_> collectionFunction;
        final List<Mapped_> list = new ArrayList<>();

        public State(IntFunction<Collection_> collectionFunction) {
            this.collectionFunction = collectionFunction;
        }

        public Collection_ result() {
            Collection_ out = collectionFunction.apply(list.size());
            if (list.isEmpty()) {
                // Avoid exception if out is an immutable collection
                return out;
            }
            out.addAll(list);
            return out;
        }
    }

    private final State<Mapped_, Result_> state;
    private Mapped_ cachedValue;

    public CustomCollectionUndoableActionable(State<Mapped_, Result_> state) {
        this.state = state;
    }

    @Override
    public void insert(Mapped_ result) {
        cachedValue = result;
        state.list.add(result);
    }

    @Override
    public void retract() {
        state.list.remove(cachedValue);
    }
}
