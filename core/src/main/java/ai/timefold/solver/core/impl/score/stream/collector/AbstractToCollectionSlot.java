package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Collection;
import java.util.function.IntFunction;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

public abstract class AbstractToCollectionSlot<Mapped_, Result_ extends Collection<Mapped_>> {
    public static final class State<Mapped_, Collection_ extends Collection<Mapped_>> {
        private final IntFunction<Collection_> collectionFunction;
        final ElementAwareArrayList<Mapped_> list = new ElementAwareArrayList<>();

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
    private ElementAwareArrayList.Entry<Mapped_> cachedEntry;

    public AbstractToCollectionSlot(State<Mapped_, Result_> state) {
        this.state = state;
    }

    protected void addMapped(Mapped_ mapped) {
        cachedEntry = state.list.addEntry(mapped);
    }

    protected void updateMapped(Mapped_ mapped) {
        cachedEntry.replaceElement(mapped);
    }

    protected void removeMapped() {
        state.list.remove(cachedEntry);
    }
}
