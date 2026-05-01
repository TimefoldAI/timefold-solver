package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.function.BinaryOperator;

public final class ReferenceSumCalculator<Result_> implements ObjectCalculator<Result_> {

    public static final class State<Result_> {
        Result_ current;
        final BinaryOperator<Result_> adder;
        final BinaryOperator<Result_> subtractor;

        public State(Result_ zero, BinaryOperator<Result_> adder, BinaryOperator<Result_> subtractor) {
            this.current = zero;
            this.adder = adder;
            this.subtractor = subtractor;
        }

        public Result_ result() {
            return current;
        }
    }

    private final State<Result_> state;
    private Result_ cachedValue;

    public ReferenceSumCalculator(State<Result_> state) {
        this.state = state;
    }

    @Override
    public void insert(Result_ input) {
        cachedValue = input;
        state.current = state.adder.apply(state.current, input);
    }

    @Override
    public void update(Result_ input) {
        if (cachedValue == input) {
            return;
        }
        state.current = state.subtractor.apply(state.current, cachedValue);
        cachedValue = input;
        state.current = state.adder.apply(state.current, input);
    }

    @Override
    public void retract() {
        state.current = state.subtractor.apply(state.current, cachedValue);
    }
}
