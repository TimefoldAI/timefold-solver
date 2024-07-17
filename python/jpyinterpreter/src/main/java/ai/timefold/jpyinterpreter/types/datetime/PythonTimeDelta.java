package ai.timefold.jpyinterpreter.types.datetime;

import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeComparable;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.errors.arithmetic.ZeroDivisionError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.numeric.PythonNumber;
import ai.timefold.jpyinterpreter.util.arguments.ArgumentSpec;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

/**
 * Python docs: <a href="https://docs.python.org/3/library/datetime.html#timedelta-objects">timedelta-objects</a>
 */
public class PythonTimeDelta extends AbstractPythonLikeObject implements PythonLikeComparable<PythonTimeDelta>,
        PlanningImmutable {
    private static final int NANOS_IN_SECOND = 1_000_000_000;
    private static final int SECONDS_IN_DAY = 86400; // 24 * 60 * 60

    public static PythonLikeType TIME_DELTA_TYPE = new PythonLikeType("timedelta",
            PythonTimeDelta.class);

    public static PythonLikeType $TYPE = TIME_DELTA_TYPE;

    static {
        try {
            PythonLikeComparable.setup(TIME_DELTA_TYPE);
            registerMethods();

            TIME_DELTA_TYPE.$setAttribute("min", new PythonTimeDelta(Duration.ofDays(-999999999)));
            TIME_DELTA_TYPE.$setAttribute("max", new PythonTimeDelta(Duration.ofDays(1000000000)
                    .minusNanos(1000)));
            TIME_DELTA_TYPE.$setAttribute("resolution", new PythonTimeDelta(Duration.ofNanos(1000)));

            PythonOverloadImplementor.createDispatchesFor(TIME_DELTA_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        TIME_DELTA_TYPE.addConstructor(ArgumentSpec.forFunctionReturning("timedelta", PythonTimeDelta.class.getName())
                .addArgument("days", PythonNumber.class.getName(), PythonInteger.ZERO)
                .addArgument("seconds", PythonNumber.class.getName(), PythonInteger.ZERO)
                .addArgument("microseconds", PythonNumber.class.getName(), PythonInteger.ZERO)
                .addArgument("milliseconds", PythonNumber.class.getName(), PythonInteger.ZERO)
                .addArgument("minutes", PythonNumber.class.getName(), PythonInteger.ZERO)
                .addArgument("hours", PythonNumber.class.getName(), PythonInteger.ZERO)
                .addArgument("weeks", PythonNumber.class.getName(), PythonInteger.ZERO)
                .asPythonFunctionSignature(PythonTimeDelta.class.getMethod("of", PythonNumber.class, PythonNumber.class,
                        PythonNumber.class, PythonNumber.class, PythonNumber.class, PythonNumber.class, PythonNumber.class)));

        // Unary
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.POSITIVE,
                PythonTimeDelta.class.getMethod("pos"));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.NEGATIVE,
                PythonTimeDelta.class.getMethod("negate"));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.ABS,
                PythonTimeDelta.class.getMethod("abs"));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.AS_BOOLEAN,
                PythonTimeDelta.class.getMethod("isZero"));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                PythonTimeDelta.class.getMethod("toPythonString"));
        TIME_DELTA_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                PythonTimeDelta.class.getMethod("toPythonRepr"));

        // Binary
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.ADD,
                PythonTimeDelta.class.getMethod("add_time_delta", PythonTimeDelta.class));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.SUBTRACT,
                PythonTimeDelta.class.getMethod("subtract_time_delta", PythonTimeDelta.class));

        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.MULTIPLY,
                PythonTimeDelta.class.getMethod("get_integer_multiple", PythonInteger.class));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.MULTIPLY,
                PythonTimeDelta.class.getMethod("get_float_multiple", PythonFloat.class));

        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.TRUE_DIVIDE,
                PythonTimeDelta.class.getMethod("divide_time_delta", PythonTimeDelta.class));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.TRUE_DIVIDE,
                PythonTimeDelta.class.getMethod("divide_integer", PythonInteger.class));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.TRUE_DIVIDE,
                PythonTimeDelta.class.getMethod("divide_float", PythonFloat.class));

        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.FLOOR_DIVIDE,
                PythonTimeDelta.class.getMethod("floor_divide_time_delta", PythonTimeDelta.class));
        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.FLOOR_DIVIDE,
                PythonTimeDelta.class.getMethod("floor_divide_integer", PythonInteger.class));

        TIME_DELTA_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonTimeDelta.class.getMethod("remainder_time_delta", PythonTimeDelta.class));

        // Methods
        TIME_DELTA_TYPE.addMethod("total_seconds", PythonTimeDelta.class.getMethod("total_seconds"));
    }

    final Duration duration;

    public final PythonInteger days;
    public final PythonInteger seconds;
    public final PythonInteger microseconds;

    public PythonTimeDelta(Duration duration) {
        super(TIME_DELTA_TYPE);
        this.duration = duration;

        if (duration.isNegative()) {
            if (duration.getSeconds() % SECONDS_IN_DAY != 0 ||
                    duration.getNano() != 0) {
                days = PythonInteger.valueOf(duration.toDays() - 1);
                seconds = PythonInteger.valueOf((SECONDS_IN_DAY + (duration.toSeconds() % SECONDS_IN_DAY) % SECONDS_IN_DAY));
            } else {
                days = PythonInteger.valueOf(duration.toDays());
                seconds = PythonInteger.valueOf(Math.abs(duration.toSeconds() % SECONDS_IN_DAY));
            }
        } else {
            days = PythonInteger.valueOf(duration.toDays());
            seconds = PythonInteger.valueOf(Math.abs(duration.toSeconds() % SECONDS_IN_DAY));
        }
        microseconds = PythonInteger.valueOf(duration.toNanosPart() / 1000);
    }

    @Override
    public PythonLikeObject $getAttributeOrNull(String name) {
        switch (name) {
            case "days":
                return days;
            case "seconds":
                return seconds;
            case "microseconds":
                return microseconds;
            default:
                return super.$getAttributeOrNull(name);
        }
    }

    public static PythonTimeDelta of(int days, int seconds, int microseconds) {
        return new PythonTimeDelta(Duration.ofDays(days).plusSeconds(seconds)
                .plusNanos(microseconds * 1000L));
    }

    public static PythonTimeDelta of(PythonNumber days, PythonNumber seconds, PythonNumber microseconds,
            PythonNumber milliseconds, PythonNumber minutes, PythonNumber hours,
            PythonNumber weeks) {
        Duration out = Duration.ZERO;
        out = addToDuration(out, days, ChronoUnit.DAYS);
        out = addToDuration(out, seconds, ChronoUnit.SECONDS);
        out = addToDuration(out, microseconds, ChronoUnit.MICROS);
        out = addToDuration(out, milliseconds, ChronoUnit.MILLIS);
        out = addToDuration(out, minutes, ChronoUnit.MINUTES);
        out = addToDuration(out, hours, ChronoUnit.HOURS);
        if (weeks instanceof PythonInteger) { // weeks is an estimated duration; cannot use addToDuration
            out = out.plusDays(weeks.getValue().longValue() * 7);
        } else if (weeks instanceof PythonFloat) {
            out = out.plusNanos(Math.round(Duration.ofDays(7L).toNanos() * weeks.getValue().doubleValue()));
        } else {
            throw new ValueError("Amount for weeks is not a float or integer.");
        }
        return new PythonTimeDelta(out);
    }

    private static Duration addToDuration(Duration duration, PythonNumber amount, TemporalUnit temporalUnit) {
        if (amount instanceof PythonInteger) {
            return duration.plus(amount.getValue().longValue(), temporalUnit);
        } else if (amount instanceof PythonFloat) {
            return duration.plusNanos(Math.round(temporalUnit.getDuration().toNanos() * amount.getValue().doubleValue()));
        } else {
            throw new IllegalArgumentException("Amount for " + temporalUnit.toString() + " is not a float or integer.");
        }
    }

    public PythonFloat total_seconds() {
        return PythonFloat.valueOf((double) duration.toNanos() / NANOS_IN_SECOND);
    }

    public PythonTimeDelta add_time_delta(PythonTimeDelta other) {
        return new PythonTimeDelta(duration.plus(other.duration));
    }

    public PythonTimeDelta subtract_time_delta(PythonTimeDelta other) {
        return new PythonTimeDelta(duration.minus(other.duration));
    }

    public PythonTimeDelta get_integer_multiple(PythonInteger multiple) {
        return new PythonTimeDelta(duration.multipliedBy(multiple.getValue().longValue()));
    }

    public PythonTimeDelta get_float_multiple(PythonFloat multiple) {
        double multipleAsDouble = multiple.getValue().doubleValue();
        long flooredMultiple = (long) Math.floor(multipleAsDouble);
        double fractionalPart = multipleAsDouble - flooredMultiple;
        long nanos = duration.toNanos();
        double fractionalNanos = fractionalPart * nanos;
        long fractionalNanosInMicroResolution = Math.round(fractionalNanos / 1000) * 1000;

        return new PythonTimeDelta(duration.multipliedBy(flooredMultiple)
                .plus(Duration.ofNanos(fractionalNanosInMicroResolution)));
    }

    public PythonFloat divide_time_delta(PythonTimeDelta divisor) {
        if (divisor.duration.equals(Duration.ZERO)) {
            throw new ZeroDivisionError("timedelta division or modulo by zero");
        }
        return PythonFloat.valueOf((double) duration.toNanos() / divisor.duration.toNanos());
    }

    public PythonTimeDelta divide_integer(PythonInteger divisor) {
        if (divisor.value.equals(BigInteger.ZERO)) {
            throw new ZeroDivisionError("timedelta division or modulo by zero");
        }
        return new PythonTimeDelta(duration.dividedBy(divisor.getValue().longValue()));
    }

    public PythonTimeDelta divide_float(PythonFloat divisor) {
        if (divisor.value == 0.0) {
            throw new ZeroDivisionError("timedelta division or modulo by zero");
        }
        double fractionalNanos = duration.toNanos() / divisor.getValue().doubleValue();
        return new PythonTimeDelta(Duration.ofNanos(Math.round(fractionalNanos / 1000) * 1000));
    }

    public PythonInteger floor_divide_time_delta(PythonTimeDelta divisor) {
        if (divisor.duration.equals(Duration.ZERO)) {
            throw new ZeroDivisionError("timedelta division or modulo by zero");
        }

        long amount = duration.dividedBy(divisor.duration);
        if (divisor.duration.multipliedBy(amount).equals(duration)) {
            // division exact
            return PythonInteger.valueOf(amount);
        }

        // division not exact
        // Java use round to zero; Python use floor
        // If both operands have the same sign, result is positive, and round to zero = floor
        // If operands have different signs, the result is negative, and round to zero = floor + 1
        if (duration.isNegative() == divisor.duration.isNegative()) {
            // same sign
            return PythonInteger.valueOf(amount);
        } else {
            // different sign
            return PythonInteger.valueOf(amount - 1);
        }
    }

    public PythonTimeDelta floor_divide_integer(PythonInteger divisor) {
        if (divisor.value.equals(BigInteger.ZERO)) {
            throw new ZeroDivisionError("timedelta division or modulo by zero");
        }
        return new PythonTimeDelta(duration.dividedBy(divisor.getValue().longValue()));
    }

    public PythonTimeDelta remainder_time_delta(PythonTimeDelta divisor) {
        boolean leftIsNegative = duration.isNegative();
        int rightHandSign = divisor.duration.compareTo(Duration.ZERO);

        if (rightHandSign == 0) {
            throw new ZeroDivisionError("timedelta division or modulo by zero");
        }

        long floorDivisionResult = duration.abs().dividedBy(divisor.abs().duration);
        Duration remainder;

        if (rightHandSign > 0) {
            // Need a positive result
            if (leftIsNegative) {
                remainder = divisor.duration.plus(duration.plus(divisor.duration.multipliedBy(floorDivisionResult)));
            } else {
                remainder = duration.minus(divisor.duration.multipliedBy(floorDivisionResult));
            }
        } else {
            // Need a negative result
            if (leftIsNegative) {
                remainder = duration.minus(divisor.duration.multipliedBy(floorDivisionResult));
            } else {
                remainder = divisor.duration.plus(duration.plus(divisor.duration.multipliedBy(floorDivisionResult)));
            }
        }
        return new PythonTimeDelta(remainder);
    }

    public PythonTimeDelta pos() {
        return this;
    }

    public PythonTimeDelta negate() {
        return new PythonTimeDelta(duration.negated());
    }

    public PythonTimeDelta abs() {
        return new PythonTimeDelta(duration.abs());
    }

    public PythonString toPythonString() {
        return new PythonString(toString());
    }

    public PythonString toPythonRepr() {
        StringBuilder out = new StringBuilder("datetime.timedelta(");
        if (!days.value.equals(BigInteger.ZERO)) {
            out.append("days=").append(days);
        }
        if (!seconds.value.equals(BigInteger.ZERO)) {
            if (out.charAt(out.length() - 1) != '(') {
                out.append(", ");
            }
            out.append("seconds=").append(seconds);
        }
        if (!microseconds.value.equals(BigInteger.ZERO)) {
            if (out.charAt(out.length() - 1) != '(') {
                out.append(", ");
            }
            out.append("microseconds=").append(microseconds);
        }

        if (out.charAt(out.length() - 1) == '(') {
            // No content; do a attribute-less zero
            out.append("0");
        }
        out.append(")");
        return PythonString.valueOf(out.toString());
    }

    public PythonBoolean isZero() {
        return PythonBoolean.valueOf(duration.isZero());
    }

    @Override
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        long daysPart = duration.toDaysPart();
        Duration durationAfterDay = duration.minusDays(daysPart);
        if (duration.isNegative() && !Duration.ofDays(1).multipliedBy(daysPart).equals(duration)) {
            daysPart = daysPart - 1;
            durationAfterDay = durationAfterDay.plus(Duration.ofDays(1));
        }

        if (daysPart != 0) {
            out.append(daysPart);
            out.append(" day");
            if (daysPart > 1 || daysPart < -1) {
                out.append('s');
            }
            out.append(", ");
        }
        int hours = durationAfterDay.toHoursPart();
        out.append(hours);
        out.append(':');

        int minutes = durationAfterDay.toMinutesPart();
        out.append(String.format("%02d", minutes));
        out.append(':');

        int seconds = durationAfterDay.toSecondsPart();
        out.append(String.format("%02d", seconds));

        int micros = durationAfterDay.toNanosPart() / 1000;
        if (micros != 0) {
            out.append(String.format(".%06d", micros));
        }

        return out.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonTimeDelta that = (PythonTimeDelta) o;
        return duration.equals(that.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }

    @Override
    public PythonString $method$__repr__() {
        return toPythonRepr();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public int compareTo(PythonTimeDelta pythonTimeDelta) {
        return duration.compareTo(pythonTimeDelta.duration);
    }
}
