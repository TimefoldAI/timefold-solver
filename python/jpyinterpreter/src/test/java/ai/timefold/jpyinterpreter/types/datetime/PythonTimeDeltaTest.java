package ai.timefold.jpyinterpreter.types.datetime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.numeric.PythonNumber;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;

public class PythonTimeDeltaTest {
    @Test
    public void testConstructTimeDeltaPositional() {
        PythonBytecodeToJavaBytecodeTranslator.classOutputRootPath = Path.of("target/generated-classes");
        PythonFunctionBuilder builder = PythonFunctionBuilder.newFunction("days")
                .loadConstant(PythonTimeDelta.TIME_DELTA_TYPE)
                .loadParameter("days")
                .callFunction(1)
                .op(ControlOpDescriptor.RETURN_VALUE);

        Function<PythonNumber, PythonTimeDelta> constructor =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(builder.build(),
                        Function.class,
                        List.of(PythonNumber.class,
                                PythonTimeDelta.class));
        assertThat(constructor.apply(PythonInteger.valueOf(30))).isEqualTo(new PythonTimeDelta(Duration.ofDays(30)));
        assertThat(constructor.apply(PythonFloat.valueOf(1.5)))
                .isEqualTo(new PythonTimeDelta(Duration.ofDays(1).plusHours(12)));
    }

    @Test
    public void testConstructTimeDeltaKeywords() {
        PythonFunctionBuilder builder = PythonFunctionBuilder.newFunction("minutes")
                .loadConstant(PythonTimeDelta.TIME_DELTA_TYPE)
                .loadParameter("minutes")
                .loadConstant("minutes")
                .tuple(1)
                .callFunctionWithKeywords(1)
                .op(ControlOpDescriptor.RETURN_VALUE);

        Function<PythonNumber, PythonTimeDelta> constructor =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(builder.build(),
                        Function.class,
                        List.of(PythonNumber.class,
                                PythonTimeDelta.class));
        assertThat(constructor.apply(PythonInteger.valueOf(30))).isEqualTo(new PythonTimeDelta(Duration.ofMinutes(30)));
        assertThat(constructor.apply(PythonFloat.valueOf(1.5)))
                .isEqualTo(new PythonTimeDelta(Duration.ofMinutes(1).plusSeconds(30)));
    }

    @Test
    public void testAddTimeDelta() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonTimeDelta b = new PythonTimeDelta(Duration.ofHours(12L));
        assertThat(a.add_time_delta(b)).isEqualTo(new PythonTimeDelta(Duration.ofDays(1L).plusHours(12L)));
    }

    @Test
    public void testSubtractTimeDelta() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonTimeDelta b = new PythonTimeDelta(Duration.ofHours(12L));
        assertThat(a.subtract_time_delta(b)).isEqualTo(new PythonTimeDelta(Duration.ofDays(1L).minusHours(12L)));
        assertThat(b.subtract_time_delta(a)).isEqualTo(new PythonTimeDelta(Duration.ofHours(12L).minusDays(1L)));
    }

    @Test
    public void testIntegerMultiplyTimeDelta() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonInteger b = PythonInteger.valueOf(3);
        assertThat(a.get_integer_multiple(b)).isEqualTo(new PythonTimeDelta(Duration.ofDays(3L)));
    }

    @Test
    public void testFloatMultiplyTimeDelta() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonFloat b = PythonFloat.valueOf(2.5);
        assertThat(a.get_float_multiple(b)).isEqualTo(new PythonTimeDelta(Duration.ofDays(2L).plusHours(12)));
    }

    @Test
    public void testDivideByTimeDelta() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonTimeDelta b = new PythonTimeDelta(Duration.ofHours(16L));
        assertThat(a.divide_time_delta(b)).isEqualTo(new PythonFloat(1.5));
    }

    @Test
    public void testDivideByInt() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonInteger b = PythonInteger.valueOf(2);
        assertThat(a.divide_integer(b)).isEqualTo(new PythonTimeDelta(Duration.ofHours(12L)));
    }

    @Test
    public void testDivideByFloat() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonFloat b = PythonFloat.valueOf(0.5);
        assertThat(a.divide_float(b)).isEqualTo(new PythonTimeDelta(Duration.ofDays(2L)));
    }

    @Test
    public void testFloorDivideByTimeDelta() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonTimeDelta b = new PythonTimeDelta(Duration.ofHours(16L));
        assertThat(a.floor_divide_time_delta(b)).isEqualTo(PythonInteger.valueOf(1));
    }

    @Test
    public void testFloorDivideByInteger() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonInteger b = PythonInteger.valueOf(2);
        assertThat(a.floor_divide_integer(b)).isEqualTo(new PythonTimeDelta(Duration.ofHours(12L)));
    }

    @Test
    public void testTimeDeltaRemainder() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        PythonTimeDelta b = new PythonTimeDelta(Duration.ofHours(16L));
        assertThat(a.remainder_time_delta(b)).isEqualTo(new PythonTimeDelta(Duration.ofHours(8L)));

        a = new PythonTimeDelta(Duration.ofHours(-15L));
        b = new PythonTimeDelta(Duration.ofHours(10L));
        assertThat(a.remainder_time_delta(b)).isEqualTo(new PythonTimeDelta(Duration.ofHours(5L)));

        a = new PythonTimeDelta(Duration.ofHours(15L));
        b = new PythonTimeDelta(Duration.ofHours(-10L));
        assertThat(a.remainder_time_delta(b)).isEqualTo(new PythonTimeDelta(Duration.ofHours(-5L)));

        a = new PythonTimeDelta(Duration.ofHours(-15L));
        b = new PythonTimeDelta(Duration.ofHours(-10L));
        assertThat(a.remainder_time_delta(b)).isEqualTo(new PythonTimeDelta(Duration.ofHours(-5L)));
    }

    @Test
    public void testTimeDeltaCopy() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        assertThat(a.pos()).isEqualTo(a);
    }

    @Test
    public void testTimeDeltaNegate() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L));
        assertThat(a.negate()).isEqualTo(new PythonTimeDelta(Duration.ofDays(-1L)));
    }

    @Test
    public void testTimeDeltaAbs() {
        assertThat(new PythonTimeDelta(Duration.ofDays(1L)).abs()).isEqualTo(new PythonTimeDelta(Duration.ofDays(1L)));
        assertThat(new PythonTimeDelta(Duration.ofDays(-1L)).abs()).isEqualTo(new PythonTimeDelta(Duration.ofDays(1L)));
    }

    @Test
    public void testTimeDeltaToString() {
        PythonTimeDelta a = new PythonTimeDelta(Duration.ofDays(1L).plusHours(6).plusMinutes(30).plusSeconds(15));
        assertThat(a.toString()).isEqualTo("1 day, 6:30:15");

        a = new PythonTimeDelta(Duration.ofHours(6).plusMinutes(30).plusSeconds(15));
        assertThat(a.toString()).isEqualTo("6:30:15");

        a = new PythonTimeDelta(Duration.ofDays(2).plusHours(6).plusMinutes(30).plusSeconds(15).plusMillis(333L));
        assertThat(a.toString()).isEqualTo("2 days, 6:30:15.333000");
    }

    @Test
    public void testCompareTimeDelta() {
        PythonBytecodeToJavaBytecodeTranslator.classOutputRootPath = Path.of("target", "generated-classes");
        PythonFunctionBuilder builder = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN)
                .op(ControlOpDescriptor.RETURN_VALUE);

        BiPredicate<PythonTimeDelta, PythonTimeDelta> lessThan =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(builder.build(),
                        BiPredicate.class,
                        List.of(PythonTimeDelta.class,
                                PythonTimeDelta.class));
        assertThat(lessThan.test(new PythonTimeDelta(Duration.ofDays(1)), new PythonTimeDelta(Duration.ofDays(2)))).isTrue();
        assertThat(lessThan.test(new PythonTimeDelta(Duration.ofDays(2)), new PythonTimeDelta(Duration.ofDays(1)))).isFalse();
    }

    @Test
    public void testEqualsTimeDelta() {
        PythonFunctionBuilder builder = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.EQUALS)
                .op(ControlOpDescriptor.RETURN_VALUE);

        BiPredicate<PythonTimeDelta, PythonTimeDelta> lessThan =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(builder.build(),
                        BiPredicate.class,
                        List.of(PythonTimeDelta.class,
                                PythonTimeDelta.class));
        assertThat(lessThan.test(new PythonTimeDelta(Duration.ofDays(1)), new PythonTimeDelta(Duration.ofDays(1)))).isTrue();
        assertThat(lessThan.test(new PythonTimeDelta(Duration.ofDays(1)), new PythonTimeDelta(Duration.ofDays(2)))).isFalse();
    }
}
