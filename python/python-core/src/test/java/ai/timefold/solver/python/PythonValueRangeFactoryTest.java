package ai.timefold.solver.python;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;

import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class PythonValueRangeFactoryTest {

    @Test
    void createIntValueRange() {
        assertThat(PythonValueRangeFactory.createIntValueRange(BigInteger.valueOf(10), BigInteger.valueOf(15)))
                .extracting(CountableValueRange::createOriginalIterator,
                        as(InstanceOfAssertFactories.iterator(PythonInteger.class)))
                .toIterable()
                .containsExactly(
                        PythonInteger.valueOf(10),
                        PythonInteger.valueOf(11),
                        PythonInteger.valueOf(12),
                        PythonInteger.valueOf(13),
                        PythonInteger.valueOf(14));
    }

    @Test
    void createIntValueRangeWithStep() {
        assertThat(PythonValueRangeFactory.createIntValueRange(BigInteger.valueOf(10), BigInteger.valueOf(20),
                BigInteger.valueOf(2)))
                .extracting(CountableValueRange::createOriginalIterator,
                        as(InstanceOfAssertFactories.iterator(PythonInteger.class)))
                .toIterable()
                .containsExactly(
                        PythonInteger.valueOf(10),
                        PythonInteger.valueOf(12),
                        PythonInteger.valueOf(14),
                        PythonInteger.valueOf(16),
                        PythonInteger.valueOf(18));
    }

    @Test
    void createFloatValueRange() {
        assertThat(PythonValueRangeFactory.createFloatValueRange(BigDecimal.valueOf(10), BigDecimal.valueOf(15)))
                .extracting(CountableValueRange::createOriginalIterator,
                        as(InstanceOfAssertFactories.iterator(PythonFloat.class)))
                .toIterable()
                .containsExactly(
                        PythonFloat.valueOf(10),
                        PythonFloat.valueOf(11),
                        PythonFloat.valueOf(12),
                        PythonFloat.valueOf(13),
                        PythonFloat.valueOf(14));
    }

    @Test
    void createFloatValueRangeWithStep() {
        assertThat(PythonValueRangeFactory.createFloatValueRange(BigDecimal.valueOf(10), BigDecimal.valueOf(20),
                BigDecimal.valueOf(2)))
                .extracting(CountableValueRange::createOriginalIterator,
                        as(InstanceOfAssertFactories.iterator(PythonFloat.class)))
                .toIterable()
                .containsExactly(
                        PythonFloat.valueOf(10),
                        PythonFloat.valueOf(12),
                        PythonFloat.valueOf(14),
                        PythonFloat.valueOf(16),
                        PythonFloat.valueOf(18));
    }

    @Test
    void createBooleanValueRange() {
        assertThat(PythonValueRangeFactory.createBooleanValueRange())
                .extracting(CountableValueRange::createOriginalIterator,
                        as(InstanceOfAssertFactories.iterator(PythonBoolean.class)))
                .toIterable()
                .containsExactly(
                        PythonBoolean.FALSE,
                        PythonBoolean.TRUE);
    }
}