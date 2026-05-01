package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ListUndoableActionable<Mapped_> implements UndoableActionable<Mapped_> {
    public static final class State<Mapped_> {
        final List<Mapped_> resultList = new ArrayList<>();

        public List<Mapped_> result() {
            return resultList;
        }
    }

    private final State<Mapped_> state;
    private Mapped_ cachedValue;

    public ListUndoableActionable(State<Mapped_> state) {
        this.state = state;
    }

    @Override
    public void insert(Mapped_ mapped) {
        cachedValue = mapped;
        state.resultList.add(mapped);
    }

    @Override
    public void update(Mapped_ mapped) {
        if (Objects.equals(cachedValue, mapped)) {
            return;
        }
        state.resultList.set(state.resultList.indexOf(cachedValue), mapped);
        cachedValue = mapped;
    }

    @Override
    public void retract() {
        state.resultList.remove(cachedValue);
    }
}
