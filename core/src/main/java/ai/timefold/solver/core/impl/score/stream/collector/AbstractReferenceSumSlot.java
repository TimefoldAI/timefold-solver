package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.function.BinaryOperator;

import org.jspecify.annotations.Nullable;

public abstract class AbstractReferenceSumSlot<Result_> {

    public static final class State<Result_> {
        private final BinaryOperator<Result_> adder;
        private final BinaryOperator<Result_> subtractor;
        private Result_ current;

        public State(Result_ zero, BinaryOperator<Result_> adder, BinaryOperator<Result_> subtractor) {
            this.adder = adder;
            this.subtractor = subtractor;
            this.current = zero;
        }

        public Result_ result() {
            return current;
        }
    }

    private final State<Result_> state;
    private @Nullable Result_ cachedValue;

    public AbstractReferenceSumSlot(State<Result_> state) {
        this.state = state;
    }

    protected void addMapped(Result_ input) {
        cachedValue = input;
        state.current = state.adder.apply(state.current, input);
    }

    protected void replaceWithMapped(Result_ input) {
        // Avoiding equals shortcut, as addition and subtraction are super-fast.
        state.current = state.adder.apply(state.subtractor.apply(state.current, cachedValue), input);
        cachedValue = input;
    }

    protected void removeMapped() {
        state.current = state.subtractor.apply(state.current, cachedValue);
    }
}
