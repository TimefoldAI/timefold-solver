package ai.timefold.solver.core.impl.score.stream.collector;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public final class ReferenceAverageCalculator<Input_, Output_> implements ObjectCalculator<Input_> {

    public static final class State<Input_, Output_> {
        int count = 0;
        Input_ sum;
        final BinaryOperator<Input_> adder;
        final BinaryOperator<Input_> subtractor;
        final BiFunction<Input_, Integer, Output_> divider;

        State(Input_ zero, BinaryOperator<Input_> adder, BinaryOperator<Input_> subtractor,
                BiFunction<Input_, Integer, Output_> divider) {
            this.sum = zero;
            this.adder = adder;
            this.subtractor = subtractor;
            this.divider = divider;
        }

        public Output_ result() {
            if (count == 0) {
                return null;
            }
            return divider.apply(sum, count);
        }
    }

    private final State<Input_, Output_> state;
    private Input_ cachedValue;

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

    public ReferenceAverageCalculator(State<Input_, Output_> state) {
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

    @Override
    public void insert(Input_ input) {
        cachedValue = input;
        state.count++;
        state.sum = state.adder.apply(state.sum, input);
    }

    @Override
    public void update(Input_ input) {
        if (cachedValue == input) {
            return;
        }
        state.sum = state.subtractor.apply(state.sum, cachedValue);
        state.sum = state.adder.apply(state.sum, input);
        cachedValue = input;
    }

    @Override
    public void retract() {
        state.count--;
        state.sum = state.subtractor.apply(state.sum, cachedValue);
    }
}
