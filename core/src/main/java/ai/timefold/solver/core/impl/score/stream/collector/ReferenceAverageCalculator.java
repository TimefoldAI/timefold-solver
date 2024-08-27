package ai.timefold.solver.core.impl.score.stream.collector;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public final class ReferenceAverageCalculator<Input_, Output_> implements ObjectCalculator<Input_, Output_, Input_> {
    int count = 0;
    Input_ sum;
    final BinaryOperator<Input_> adder;
    final BinaryOperator<Input_> subtractor;
    final BiFunction<Input_, Integer, Output_> divider;

    private final static Supplier<ReferenceAverageCalculator<BigDecimal, BigDecimal>> BIG_DECIMAL =
            () -> new ReferenceAverageCalculator<>(BigDecimal.ZERO, BigDecimal::add, BigDecimal::subtract,
                    (sum, count) -> sum.divide(BigDecimal.valueOf(count), RoundingMode.HALF_EVEN));

    private final static Supplier<ReferenceAverageCalculator<BigInteger, BigDecimal>> BIG_INTEGER =
            () -> new ReferenceAverageCalculator<>(BigInteger.ZERO, BigInteger::add, BigInteger::subtract,
                    (sum, count) -> new BigDecimal(sum).divide(BigDecimal.valueOf(count), RoundingMode.HALF_EVEN));

    private final static Supplier<ReferenceAverageCalculator<Duration, Duration>> DURATION =
            () -> new ReferenceAverageCalculator<>(Duration.ZERO, Duration::plus, Duration::minus,
                    (sum, count) -> {
                        long nanos = sum.toNanos();
                        return Duration.ofNanos(nanos / count);
                    });

    public ReferenceAverageCalculator(Input_ zero, BinaryOperator<Input_> adder, BinaryOperator<Input_> subtractor,
            BiFunction<Input_, Integer, Output_> divider) {
        this.sum = zero;
        this.adder = adder;
        this.subtractor = subtractor;
        this.divider = divider;
    }

    public static Supplier<ReferenceAverageCalculator<BigDecimal, BigDecimal>> bigDecimal() {
        return BIG_DECIMAL;
    }

    public static Supplier<ReferenceAverageCalculator<BigInteger, BigDecimal>> bigInteger() {
        return BIG_INTEGER;
    }

    public static Supplier<ReferenceAverageCalculator<Duration, Duration>> duration() {
        return DURATION;
    }

    @Override
    public Input_ insert(Input_ input) {
        count++;
        sum = adder.apply(sum, input);
        return input;
    }

    @Override
    public void retract(Input_ mapped) {
        count--;
        sum = subtractor.apply(sum, mapped);
    }

    @Override
    public Output_ result() {
        if (count == 0) {
            return null;
        }
        return divider.apply(sum, count);
    }
}
