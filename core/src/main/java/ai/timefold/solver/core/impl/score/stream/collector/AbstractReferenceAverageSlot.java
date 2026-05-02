package ai.timefold.solver.core.impl.score.stream.collector;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

public abstract class AbstractReferenceAverageSlot<Input_, Output_> {

    public static final class State<Input_, Output_> {
        private final BinaryOperator<Input_> adder;
        private final BinaryOperator<Input_> subtractor;
        private final BiFunction<Input_, Integer, Output_> divider;
        private int count = 0;
        private Input_ sum;

        State(Input_ zero, BinaryOperator<Input_> adder, BinaryOperator<Input_> subtractor,
                BiFunction<Input_, Integer, Output_> divider) {
            this.adder = adder;
            this.subtractor = subtractor;
            this.divider = divider;
            this.sum = zero;
        }

        public Output_ result() {
            if (count == 0) {
                return null;
            }
            return divider.apply(sum, count);
        }
    }

    private final State<Input_, Output_> state;
    private @Nullable Input_ cachedValue;

    private final static Supplier<State<BigDecimal, BigDecimal>> BIG_DECIMAL =
            () -> new State<>(BigDecimal.ZERO, BigDecimal::add, BigDecimal::subtract,
                    (sum, count) -> sum.divide(BigDecimal.valueOf(count), RoundingMode.HALF_EVEN));

    private final static Supplier<State<BigInteger, BigDecimal>> BIG_INTEGER =
            () -> new State<>(BigInteger.ZERO, BigInteger::add, BigInteger::subtract,
                    (sum, count) -> new BigDecimal(sum).divide(BigDecimal.valueOf(count), RoundingMode.HALF_EVEN));

    private final static Supplier<State<Duration, Duration>> DURATION =
            () -> new State<>(Duration.ZERO, Duration::plus, Duration::minus,
                    (sum, count) -> {
                        long nanos = sum.toNanos();
                        return Duration.ofNanos(nanos / count);
                    });

    public AbstractReferenceAverageSlot(State<Input_, Output_> state) {
        this.state = state;
    }

    public static Supplier<State<BigDecimal, BigDecimal>> bigDecimalState() {
        return BIG_DECIMAL;
    }

    public static Supplier<State<BigInteger, BigDecimal>> bigIntegerState() {
        return BIG_INTEGER;
    }

    public static Supplier<State<Duration, Duration>> durationState() {
        return DURATION;
    }

    protected void addMapped(Input_ input) {
        cachedValue = input;
        state.count++;
        state.sum = state.adder.apply(state.sum, input);
    }

    protected void updateMapped(Input_ input) {
        if (cachedValue == input) {
            return;
        }
        state.sum = state.subtractor.apply(state.sum, cachedValue);
        state.sum = state.adder.apply(state.sum, input);
        cachedValue = input;
    }

    protected void removeMapped() {
        state.count--;
        state.sum = state.subtractor.apply(state.sum, cachedValue);
    }
}
