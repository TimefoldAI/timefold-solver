package ai.timefold.jpyinterpreter.types.numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import ai.timefold.jpyinterpreter.PythonClassTranslator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

public class PythonDecimal extends AbstractPythonLikeObject implements PythonNumber, PlanningImmutable {
    public final BigDecimal value;
    private static final ThreadLocal<MathContext> threadMathContext =
            ThreadLocal.withInitial(() -> new MathContext(28, RoundingMode.HALF_EVEN));

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonDecimal::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        BuiltinTypes.DECIMAL_TYPE.setConstructor((positionalArguments, namedArguments, callerInstance) -> {
            if (positionalArguments.size() == 0) {
                return new PythonDecimal(BigDecimal.ZERO);
            } else if (positionalArguments.size() == 1) {
                return PythonDecimal.from(positionalArguments.get(0));
            } else if (positionalArguments.size() == 2) {
                // TODO: Support context
                throw new ValueError("context constructor not supported");
            } else {
                throw new TypeError("function takes at most 2 arguments, got " + positionalArguments.size());
            }
        });

        for (var method : PythonDecimal.class.getDeclaredMethods()) {
            if (method.getName().startsWith(PythonClassTranslator.JAVA_METHOD_PREFIX)) {
                BuiltinTypes.DECIMAL_TYPE.addMethod(
                        method.getName().substring(PythonClassTranslator.JAVA_METHOD_PREFIX.length()),
                        method);
            }
        }

        return BuiltinTypes.DECIMAL_TYPE;
    }

    // ***************************
    // Constructors
    // ***************************
    public PythonDecimal(BigDecimal value) {
        super(BuiltinTypes.DECIMAL_TYPE);
        this.value = value;
    }

    public static PythonDecimal from(PythonLikeObject value) {
        if (value instanceof PythonInteger integer) {
            return PythonDecimal.valueOf(integer);
        } else if (value instanceof PythonFloat pythonFloat) {
            return PythonDecimal.valueOf(pythonFloat);
        } else if (value instanceof PythonString str) {
            return PythonDecimal.valueOf(str);
        } else {
            throw new TypeError(
                    "conversion from %s to Decimal is not supported".formatted(value.$getType().getTypeName()));
        }
    }

    public static PythonDecimal $method$from_float(PythonFloat value) {
        return new PythonDecimal(new BigDecimal(value.value, threadMathContext.get()));
    }

    public static PythonDecimal valueOf(PythonInteger value) {
        return new PythonDecimal(new BigDecimal(value.value, threadMathContext.get()));
    }

    public static PythonDecimal valueOf(PythonFloat value) {
        return new PythonDecimal(new BigDecimal(value.value, threadMathContext.get()));
    }

    public static PythonDecimal valueOf(PythonString value) {
        return valueOf(value.value);
    }

    public static PythonDecimal valueOf(String value) {
        return new PythonDecimal(new BigDecimal(value, threadMathContext.get()));
    }

    // ***************************
    // Interface methods
    // ***************************

    @Override
    public Number getValue() {
        return value;
    }

    @Override
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public PythonString $method$__repr__() {
        return PythonString.valueOf("Decimal('%s')".formatted(value.toPlainString()));
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }

    public boolean equals(Object o) {
        if (o instanceof PythonNumber number) {
            return compareTo(number) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return $method$__hash__().value.intValue();
    }

    @Override
    public PythonInteger $method$__hash__() {
        var scale = value.scale();
        if (scale <= 0) {
            return PythonNumber.computeHash(new PythonInteger(value.toBigInteger()),
                    PythonInteger.ONE);
        }
        var scaledValue = value.movePointRight(scale);
        return PythonNumber.computeHash(new PythonInteger(scaledValue.toBigInteger()),
                new PythonInteger(BigInteger.TEN.pow(scale)));
    }

    // ***************************
    // Unary operations
    // ***************************
    public PythonBoolean $method$__bool__() {
        return PythonBoolean.valueOf(value.compareTo(BigDecimal.ZERO) != 0);
    }

    public PythonInteger $method$__int__() {
        return PythonInteger.valueOf(value.toBigInteger());
    }

    public PythonFloat $method$__float__() {
        return PythonFloat.valueOf(value.doubleValue());
    }

    public PythonDecimal $method$__pos__() {
        return this;
    }

    public PythonDecimal $method$__neg__() {
        return new PythonDecimal(value.negate());
    }

    public PythonDecimal $method$__abs__() {
        return new PythonDecimal(value.abs());
    }

    // ***************************
    // Binary operations
    // ***************************
    public PythonBoolean $method$__lt__(PythonDecimal other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) < 0);
    }

    public PythonBoolean $method$__lt__(PythonInteger other) {
        return $method$__lt__(PythonDecimal.valueOf(other));
    }

    public PythonBoolean $method$__lt__(PythonFloat other) {
        return $method$__lt__(PythonDecimal.valueOf(other));
    }

    public PythonBoolean $method$__le__(PythonDecimal other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) <= 0);
    }

    public PythonBoolean $method$__le__(PythonInteger other) {
        return $method$__le__(PythonDecimal.valueOf(other));
    }

    public PythonBoolean $method$__le__(PythonFloat other) {
        return $method$__le__(PythonDecimal.valueOf(other));
    }

    public PythonBoolean $method$__gt__(PythonDecimal other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) > 0);
    }

    public PythonBoolean $method$__gt__(PythonInteger other) {
        return $method$__gt__(PythonDecimal.valueOf(other));
    }

    public PythonBoolean $method$__gt__(PythonFloat other) {
        return $method$__gt__(PythonDecimal.valueOf(other));
    }

    public PythonBoolean $method$__ge__(PythonDecimal other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) >= 0);
    }

    public PythonBoolean $method$__ge__(PythonInteger other) {
        return $method$__ge__(PythonDecimal.valueOf(other));
    }

    public PythonBoolean $method$__ge__(PythonFloat other) {
        return $method$__ge__(PythonDecimal.valueOf(other));
    }

    public PythonBoolean $method$__eq__(PythonDecimal other) {
        return PythonBoolean.valueOf(value.compareTo(other.value) == 0);
    }

    public PythonBoolean $method$__eq__(PythonInteger other) {
        return PythonBoolean.valueOf(value.compareTo(new BigDecimal(other.value)) == 0);
    }

    public PythonBoolean $method$__eq__(PythonFloat other) {
        return PythonBoolean.valueOf(value.compareTo(new BigDecimal(other.value)) == 0);
    }

    public PythonBoolean $method$__neq__(PythonDecimal other) {
        return $method$__eq__(other).not();
    }

    public PythonBoolean $method$__neq__(PythonInteger other) {
        return $method$__eq__(other).not();
    }

    public PythonBoolean $method$__neq__(PythonFloat other) {
        return $method$__eq__(other).not();
    }

    public PythonDecimal $method$__add__(PythonDecimal other) {
        return new PythonDecimal(value.add(other.value, threadMathContext.get()));
    }

    public PythonDecimal $method$__add__(PythonInteger other) {
        return $method$__add__(PythonDecimal.valueOf(other));
    }

    public PythonDecimal $method$__radd__(PythonInteger other) {
        return PythonDecimal.valueOf(other).$method$__add__(this);
    }

    public PythonDecimal $method$__sub__(PythonDecimal other) {
        return new PythonDecimal(value.subtract(other.value, threadMathContext.get()));
    }

    public PythonDecimal $method$__sub__(PythonInteger other) {
        return $method$__sub__(PythonDecimal.valueOf(other));
    }

    public PythonDecimal $method$__rsub__(PythonInteger other) {
        return PythonDecimal.valueOf(other).$method$__sub__(this);
    }

    public PythonDecimal $method$__mul__(PythonDecimal other) {
        return new PythonDecimal(value.multiply(other.value, threadMathContext.get()));
    }

    public PythonDecimal $method$__mul__(PythonInteger other) {
        return $method$__mul__(PythonDecimal.valueOf(other));
    }

    public PythonDecimal $method$__rmul__(PythonInteger other) {
        return PythonDecimal.valueOf(other).$method$__mul__(this);
    }

    public PythonDecimal $method$__truediv__(PythonDecimal other) {
        return new PythonDecimal(value.divide(other.value, threadMathContext.get()));
    }

    public PythonDecimal $method$__truediv__(PythonInteger other) {
        return $method$__truediv__(PythonDecimal.valueOf(other));
    }

    public PythonDecimal $method$__rtruediv__(PythonInteger other) {
        return PythonDecimal.valueOf(other).$method$__truediv__(this);
    }

    public PythonDecimal $method$__floordiv__(PythonDecimal other) {
        var newSignNum = switch (value.signum() * other.value.signum()) {
            case -1 -> BigDecimal.ONE.negate();
            case 0 -> BigDecimal.ZERO;
            case 1 -> BigDecimal.ONE;
            default -> throw new IllegalStateException("Unexpected signum (%d)."
                    .formatted(value.signum() * other.value.signum()));
        };
        // Need to round toward 0, but Java floors the result, so take the absolute and
        // multiply by the sign-num
        return new PythonDecimal(value.abs().divideToIntegralValue(other.value.abs())
                .multiply(newSignNum, threadMathContext.get()));
    }

    public PythonDecimal $method$__floordiv__(PythonInteger other) {
        return $method$__floordiv__(PythonDecimal.valueOf(other));
    }

    public PythonDecimal $method$__rfloordiv__(PythonInteger other) {
        return PythonDecimal.valueOf(other).$method$__floordiv__(this);
    }

    public PythonDecimal $method$__mod__(PythonDecimal other) {
        return new PythonDecimal(
                value.subtract($method$__floordiv__(other).value.multiply(other.value, threadMathContext.get())));
    }

    public PythonDecimal $method$__mod__(PythonInteger other) {
        return $method$__mod__(PythonDecimal.valueOf(other));
    }

    public PythonDecimal $method$__rmod__(PythonInteger other) {
        return PythonDecimal.valueOf(other).$method$__mod__(this);
    }

    public PythonDecimal $method$__pow__(PythonDecimal other) {
        if (other.value.stripTrailingZeros().scale() <= 0) {
            // other is an int
            return new PythonDecimal(value.pow(other.value.intValue(), threadMathContext.get()));
        }
        return new PythonDecimal(new BigDecimal(Math.pow(value.doubleValue(), other.value.doubleValue()),
                threadMathContext.get()));
    }

    public PythonDecimal $method$__pow__(PythonInteger other) {
        return $method$__pow__(PythonDecimal.valueOf(other));
    }

    public PythonDecimal $method$__rpow__(PythonInteger other) {
        return PythonDecimal.valueOf(other).$method$__mod__(this);
    }

    // ***************************
    // Other methods
    // ***************************
    public PythonInteger $method$adjusted() {
        // scale is the negative exponent that the big int is multiplied by
        // len(unscaled) - 1 = floor(log_10(unscaled))
        // floor(log_10(unscaled)) - scale = exponent in engineering notation
        return PythonInteger.valueOf(value.unscaledValue().toString().length() - 1 - value.scale());
    }

    public PythonLikeTuple<PythonInteger> $method$as_integer_ratio() {
        var parts = value.divideAndRemainder(BigDecimal.ONE);
        var integralPart = parts[0];
        var fractionPart = parts[1];
        if (fractionPart.compareTo(BigDecimal.ZERO) == 0) {
            // No decimal part, as integer ratio = (self, 1)
            return PythonLikeTuple.fromItems(PythonInteger.valueOf(integralPart.toBigInteger()),
                    PythonInteger.ONE);
        }
        var scale = fractionPart.scale();
        var scaledDenominator = BigDecimal.ONE.movePointRight(scale).toBigInteger();
        var scaledIntegralPart = integralPart.movePointRight(scale).toBigInteger();
        var scaledFractionPart = fractionPart.movePointRight(scale).toBigInteger();
        var scaledNumerator = scaledIntegralPart.add(scaledFractionPart);
        var commonFactors = scaledNumerator.gcd(scaledDenominator);
        var reducedNumerator = scaledNumerator.divide(commonFactors);
        var reducedDenominator = scaledDenominator.divide(commonFactors);
        return PythonLikeTuple.fromItems(PythonInteger.valueOf(reducedNumerator),
                PythonInteger.valueOf(reducedDenominator));
    }

    public PythonLikeTuple<PythonLikeObject> $method$as_tuple() {
        // TODO: Use named tuple
        return PythonLikeTuple.fromItems(PythonInteger.valueOf(value.signum() >= 0 ? 0 : 1),
                value.unscaledValue().abs().toString()
                        .chars()
                        .mapToObj(digit -> PythonInteger.valueOf(digit - '0'))
                        .collect(Collectors.toCollection(PythonLikeTuple::new)),
                PythonInteger.valueOf(-value.scale()));
    }

    public PythonDecimal $method$canonical() {
        return this;
    }

    public PythonDecimal $method$compare(PythonDecimal other) {
        return new PythonDecimal(BigDecimal.valueOf(value.compareTo(other.value)));
    }

    public PythonDecimal $method$compare_signal(PythonDecimal other) {
        return $method$compare(other);
    }

    // See https://speleotrove.com/decimal/damisc.html#refcotot
    public PythonDecimal $method$compare_total(PythonDecimal other) {
        var result = $method$compare(other);
        if (result.value.compareTo(BigDecimal.ZERO) != 0) {
            return result;
        }
        var sigNum = value.scale() - other.value.scale();
        if (sigNum < 0) {
            return new PythonDecimal(BigDecimal.ONE);
        }
        if (sigNum > 0) {
            return new PythonDecimal(BigDecimal.valueOf(-1L));
        }
        return result; // Can only reach here if result == BigDecimal.ZERO
    }

    public PythonDecimal $method$compare_total_mag(PythonDecimal other) {
        return new PythonDecimal(value.abs()).$method$compare_total(new PythonDecimal(other.value.abs()));
    }

    public PythonDecimal $method$conjugate() {
        return this;
    }

    public PythonDecimal $method$copy_abs() {
        return new PythonDecimal(value.abs());
    }

    public PythonDecimal $method$copy_negate() {
        return new PythonDecimal(value.negate());
    }

    public PythonDecimal $method$copy_sign(PythonDecimal other) {
        var signChange = value.signum() * other.value.signum();
        var multiplier = switch (signChange) {
            case -1 -> BigDecimal.valueOf(-1);
            case 0, 1 -> BigDecimal.ONE; // Note: there also a -0 BigDecimal in Python.
            default -> throw new IllegalStateException("Unexpected signum (%d).".formatted(signChange));
        };
        return new PythonDecimal(value.multiply(multiplier));
    }

    private static BigDecimal getEToPrecision(int precision) {
        return getESubPowerToPrecision(BigDecimal.ONE, precision);
    }

    private static BigDecimal getESubPowerToPrecision(BigDecimal value, int precision) {
        // Uses taylor series e^x = sum(x^n/n! for n in 0...infinity)
        var numerator = BigDecimal.ONE;
        var denominator = BigDecimal.ONE;
        var total = BigDecimal.ZERO;
        var extendedContext = new MathContext(precision + 8, RoundingMode.HALF_EVEN);
        for (var index = 1; index < 100; index++) {
            total = total.add(numerator.divide(denominator, extendedContext), extendedContext);
            numerator = numerator.multiply(value);
            denominator = denominator.multiply(BigDecimal.valueOf(index));
        }
        return total;
    }

    private static BigDecimal getEPower(BigDecimal value, int precision) {
        var extendedPrecision = precision + 8;

        // Do e^x = e^(int(x))*e^(frac(x))
        var e = getEToPrecision(extendedPrecision);
        var integralPart = value.toBigInteger().intValue();
        var fractionPart = value.remainder(BigDecimal.ONE);
        return e.pow(integralPart).multiply(getESubPowerToPrecision(fractionPart, extendedPrecision),
                threadMathContext.get());
    }

    public PythonDecimal $method$exp() {
        var precision = threadMathContext.get().getPrecision();
        return new PythonDecimal(getEPower(value, precision));
    }

    public PythonDecimal $method$fma(PythonDecimal multiplier, PythonDecimal summand) {
        return new PythonDecimal(this.value.multiply(multiplier.value).add(summand.value, threadMathContext.get()));
    }

    public PythonDecimal $method$fma(PythonInteger multiplier, PythonDecimal summand) {
        return $method$fma(PythonDecimal.valueOf(multiplier), summand);
    }

    public PythonDecimal $method$fma(PythonDecimal multiplier, PythonInteger summand) {
        return $method$fma(multiplier, PythonDecimal.valueOf(summand));
    }

    public PythonDecimal $method$fma(PythonInteger multiplier, PythonInteger summand) {
        return $method$fma(PythonDecimal.valueOf(multiplier), PythonDecimal.valueOf(summand));
    }

    public PythonBoolean $method$is_canonical() {
        return PythonBoolean.TRUE;
    }

    public PythonBoolean $method$is_finite() {
        // We don't support infinite or NaN Decimals
        return PythonBoolean.TRUE;
    }

    public PythonBoolean $method$is_infinite() {
        // We don't support infinite or NaN Decimals
        return PythonBoolean.FALSE;
    }

    public PythonBoolean $method$is_nan() {
        // We don't support infinite or NaN Decimals
        return PythonBoolean.FALSE;
    }

    public PythonBoolean $method$is_normal() {
        // We don't support subnormal Decimals
        return PythonBoolean.TRUE;
    }

    public PythonBoolean $method$is_qnan() {
        // We don't support infinite or NaN Decimals
        return PythonBoolean.FALSE;
    }

    public PythonBoolean $method$is_signed() {
        // Same as `isNegative()`
        return value.compareTo(BigDecimal.ZERO) < 0 ? PythonBoolean.TRUE : PythonBoolean.FALSE;
    }

    public PythonBoolean $method$is_snan() {
        // We don't support infinite or NaN Decimals
        return PythonBoolean.FALSE;
    }

    public PythonBoolean $method$is_subnormal() {
        // We don't support subnormal Decimals
        return PythonBoolean.FALSE;
    }

    public PythonBoolean $method$is_zero() {
        return value.compareTo(BigDecimal.ZERO) == 0 ? PythonBoolean.TRUE : PythonBoolean.FALSE;
    }

    public PythonDecimal $method$ln() {
        return new PythonDecimal(new BigDecimal(
                Math.log(value.doubleValue()),
                threadMathContext.get()));
    }

    public PythonDecimal $method$log10() {
        return new PythonDecimal(new BigDecimal(
                Math.log10(value.doubleValue()),
                threadMathContext.get()));
    }

    public PythonDecimal $method$logb() {
        // Finds the exponent b in a * 10^b, where a in [1, 10)
        return new PythonDecimal(BigDecimal.valueOf(value.precision() - value.scale() - 1));
    }

    private static PythonDecimal logicalOp(BiPredicate<Boolean, Boolean> op,
            BigDecimal a, BigDecimal b) {
        if (a.scale() < 0 || b.scale() < 0) {
            throw new ValueError("Invalid Operation: both operands must be positive integers consisting of 1's and 0's");
        }
        var aText = a.toPlainString();
        var bText = b.toPlainString();
        if (aText.length() > bText.length()) {
            bText = "0".repeat(aText.length() - bText.length()) + bText;
        } else if (aText.length() < bText.length()) {
            aText = "0".repeat(bText.length() - aText.length()) + aText;
        }

        var digitCount = aText.length();
        var result = new StringBuilder();
        for (int i = 0; i < digitCount; i++) {
            var aBit = switch (aText.charAt(i)) {
                case '0' -> false;
                case '1' -> true;
                default -> throw new ValueError(("Invalid Operation: first operand (%s) is not a positive integer " +
                        "consisting of 1's and 0's").formatted(a));
            };
            var bBit = switch (bText.charAt(i)) {
                case '0' -> false;
                case '1' -> true;
                default -> throw new ValueError(("Invalid Operation: second operand (%s) is not a positive integer " +
                        "consisting of 1's and 0's").formatted(b));
            };
            result.append(op.test(aBit, bBit) ? '1' : '0');
        }
        return new PythonDecimal(new BigDecimal(result.toString()));
    }

    public PythonDecimal $method$logical_and(PythonDecimal other) {
        return logicalOp(Boolean::logicalAnd, this.value, other.value);
    }

    public PythonDecimal $method$logical_or(PythonDecimal other) {
        return logicalOp(Boolean::logicalOr, this.value, other.value);
    }

    public PythonDecimal $method$logical_xor(PythonDecimal other) {
        return logicalOp(Boolean::logicalXor, this.value, other.value);
    }

    public PythonDecimal $method$logical_invert() {
        return logicalOp(Boolean::logicalXor, this.value, new BigDecimal("1".repeat(threadMathContext.get().getPrecision())));
    }

    public PythonDecimal $method$max(PythonDecimal other) {
        return new PythonDecimal(value.max(other.value));
    }

    public PythonDecimal $method$max_mag(PythonDecimal other) {
        var result = $method$compare_total_mag(other).value.intValue();
        if (result >= 0) {
            return this;
        } else {
            return other;
        }
    }

    public PythonDecimal $method$min(PythonDecimal other) {
        return new PythonDecimal(value.min(other.value));
    }

    public PythonDecimal $method$min_mag(PythonDecimal other) {
        var result = $method$compare_total_mag(other).value.intValue();
        if (result <= 0) {
            return this;
        } else {
            return other;
        }
    }

    private BigDecimal getLastPlaceUnit(MathContext mathContext) {
        int remainingPrecision = mathContext.getPrecision() - value.stripTrailingZeros().precision();
        return BigDecimal.ONE.movePointLeft(value.scale() + remainingPrecision + 1);
    }

    public PythonDecimal $method$next_minus() {
        var context = new MathContext(threadMathContext.get().getPrecision(), RoundingMode.FLOOR);
        var lastPlaceUnit = getLastPlaceUnit(context);
        return new PythonDecimal(value.subtract(lastPlaceUnit, context));
    }

    public PythonDecimal $method$next_plus() {
        var context = new MathContext(threadMathContext.get().getPrecision(), RoundingMode.CEILING);
        var lastPlaceUnit = getLastPlaceUnit(context);
        return new PythonDecimal(value.add(lastPlaceUnit, context));
    }

    public PythonDecimal $method$next_toward(PythonDecimal other) {
        var result = $method$compare(other).value.intValue();
        switch (result) {
            case -1 -> {
                return $method$next_plus();
            }
            case 1 -> {
                return $method$next_minus();
            }
            case 0 -> {
                return this;
            }
            default -> throw new IllegalStateException();
        }
    }

    public PythonDecimal $method$normalize() {
        return new PythonDecimal(value.stripTrailingZeros());
    }

    public PythonString $method$number_class() {
        var result = value.compareTo(BigDecimal.ZERO);
        if (result < 0) {
            return PythonString.valueOf("-Normal");
        } else if (result > 0) {
            return PythonString.valueOf("+Normal");
        } else {
            return PythonString.valueOf("+Zero");
        }
    }

    public PythonDecimal $method$quantize(PythonDecimal other) {
        return new PythonDecimal(value.setScale(other.value.scale(), threadMathContext.get().getRoundingMode()));
    }

    public PythonDecimal $method$radix() {
        return new PythonDecimal(BigDecimal.TEN);
    }

    public PythonDecimal $method$remainder_near(PythonDecimal other) {
        var floorQuotient = $method$__floordiv__(other).value;
        var firstRemainder = new PythonDecimal(value.subtract(floorQuotient.multiply(other.value, threadMathContext.get())));
        var secondRemainder = other.$method$__sub__(firstRemainder).$method$__neg__();
        var comparison = firstRemainder.$method$compare_total_mag(secondRemainder).value.intValue();
        return switch (comparison) {
            case -1 -> firstRemainder;
            case 1 -> secondRemainder;
            case 0 -> {
                if (floorQuotient.longValue() % 2 == 0) {
                    yield firstRemainder;
                } else {
                    yield secondRemainder;
                }
            }
            default -> throw new IllegalStateException();
        };
    }

    public PythonDecimal $method$rotate(PythonInteger other) {
        var amount = -other.value.intValue();
        if (amount == 0) {
            return this;
        }
        var precision = threadMathContext.get().getPrecision();
        if (Math.abs(amount) > precision) {
            throw new ValueError("other must be between -%d and %d".formatted(amount, amount));
        }
        var digitString = value.unscaledValue().toString();
        digitString = "0".repeat(precision - digitString.length()) + digitString;
        if (amount < 0) {
            // Turn a rotate right to a rotate left
            amount = precision + amount;
        }
        var rotatedResult = digitString.substring(precision - amount, precision) + digitString.substring(0, precision - amount);
        var unscaledResult = new BigInteger(rotatedResult);
        return new PythonDecimal(new BigDecimal(unscaledResult, value.scale()));
    }

    public PythonBoolean $method$same_quantum(PythonDecimal other) {
        return PythonBoolean.valueOf(
                value.ulp().compareTo(other.value.ulp()) == 0);
    }

    public PythonDecimal $method$scaleb(PythonInteger other) {
        return new PythonDecimal(value.movePointRight(other.value.intValue()));
    }

    public PythonDecimal $method$shift(PythonInteger other) {
        var amount = other.value.intValue();
        if (amount == 0) {
            return this;
        }
        var precision = threadMathContext.get().getPrecision();
        if (Math.abs(amount) > precision) {
            throw new ValueError("other must be between -%d and %d".formatted(amount, amount));
        }
        return new PythonDecimal(value.movePointLeft(amount));
    }

    public PythonDecimal $method$sqrt() {
        return new PythonDecimal(value.sqrt(threadMathContext.get()));
    }

    public PythonString $method$to_eng_string() {
        return new PythonString(value.toEngineeringString());
    }

    public PythonInteger $method$to_integral() {
        return $method$to_integral_value();
    }

    public PythonInteger $method$to_integral_exact() {
        // TODO: set signals in the context object
        return $method$to_integral_value();
    }

    public PythonInteger $method$to_integral_value() {
        return new PythonInteger(value.divideToIntegralValue(BigDecimal.ONE, threadMathContext.get()).toBigInteger());
    }

    public PythonInteger $method$__round__() {
        // Round without an argument ignores thread math context
        var first = value.toBigInteger();
        var second = first.add(BigInteger.ONE);
        var firstDiff = value.subtract(new BigDecimal(first));
        var secondDiff = new BigDecimal(second).subtract(value);
        var comparison = firstDiff.compareTo(secondDiff);
        return switch (comparison) {
            case -1 -> new PythonInteger(first);
            case 1 -> new PythonInteger(second);
            case 0 -> {
                if (first.intValue() % 2 == 0) {
                    yield new PythonInteger(first);
                } else {
                    yield new PythonInteger(second);
                }
            }
            default -> throw new IllegalStateException();
        };
    }

    public PythonLikeObject $method$__round__(PythonLikeObject maybePrecision) {
        if (maybePrecision instanceof PythonNone) {
            return $method$__round__();
        }
        if (!(maybePrecision instanceof PythonInteger precision)) {
            throw new ValueError("ndigits must be an integer");
        }
        // Round with an argument uses thread math context
        var integralPart = value.toBigInteger();
        return new PythonDecimal(value.round(new MathContext(
                integralPart.toString().length() + precision.value.intValue(),
                threadMathContext.get().getRoundingMode())));
    }
}
