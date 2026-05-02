package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.consecutive.ConsecutiveSetTree;

import org.jspecify.annotations.Nullable;

public abstract class AbstractSequenceSlot<Result_> {

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
    private @Nullable Result_ cachedValue;

    public AbstractSequenceSlot(State<Result_> state) {
        this.state = state;
    }

    protected void addMapped(Result_ result) {
        cachedValue = result;
        state.context.add(result, state.toIndexFunction.applyAsInt(result));
    }

    protected void updateMapped(Result_ input) {
        state.context.remove(cachedValue);
        cachedValue = input;
        state.context.add(input, state.toIndexFunction.applyAsInt(input));
    }

    protected void removeMapped() {
        state.context.remove(cachedValue);
    }
}
