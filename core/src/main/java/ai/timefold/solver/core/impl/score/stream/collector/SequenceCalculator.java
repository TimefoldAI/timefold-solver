package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.consecutive.ConsecutiveSetTree;

public final class SequenceCalculator<Result_>
        implements ObjectCalculator<Result_> {

    public static final class State<Result_> {
        private static final BinaryOperator<Integer> DIFFERENCE = (a, b) -> b - a;
        private final ConsecutiveSetTree<Result_, Integer, Integer> context =
                new ConsecutiveSetTree<>(DIFFERENCE, Integer::sum, 1, 0);
        private final ToIntFunction<Result_> toIndexFunction;

        public State(ToIntFunction<Result_> toIndexFunction) {
            this.toIndexFunction = Objects.requireNonNull(toIndexFunction);
        }

        public SequenceChain<Result_, Integer> result() {
            return context;
        }
    }

    private final State<Result_> state;
    private Result_ cachedValue;

    public SequenceCalculator(State<Result_> state) {
        this.state = state;
    }

    @Override
    public void insert(Result_ result) {
        cachedValue = result;
        state.context.add(result, state.toIndexFunction.applyAsInt(result));
    }

    @Override
    public void update(Result_ input) {
        state.context.remove(cachedValue);
        cachedValue = input;
        state.context.add(input, state.toIndexFunction.applyAsInt(input));
    }

    @Override
    public void retract() {
        state.context.remove(cachedValue);
    }
}
