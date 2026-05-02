package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.List;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

public abstract class AbstractToListSlot<Mapped_> {
    public static final class State<Mapped_> {
        final ElementAwareArrayList<Mapped_> resultList = new ElementAwareArrayList<>();

        public List<Mapped_> result() {
            return resultList;
        }
    }

    private final State<Mapped_> state;
    private ElementAwareArrayList.Entry<Mapped_> cachedEntry;

    public AbstractToListSlot(State<Mapped_> state) {
        this.state = state;
    }

    protected void addMapped(Mapped_ mapped) {
        cachedEntry = state.resultList.addEntry(mapped);
    }

    protected void updateMapped(Mapped_ mapped) {
        cachedEntry.replaceElement(mapped);
    }

    protected void removeMapped() {
        state.resultList.remove(cachedEntry);
        cachedEntry = null;
    }
}
