package ai.timefold.solver.python;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;

@SuppressWarnings("unused")
public class PythonValueRangeFactory {
    private PythonValueRangeFactory() {
    }

    private record IteratorMapper<From_, To_>(Iterator<From_> sourceIterator, Function<From_, To_> valueConvertor)
            implements
                Iterator<To_> {

        @Override
        public boolean hasNext() {
            return sourceIterator.hasNext();
        }

        @Override
        public To_ next() {
            return valueConvertor.apply(sourceIterator.next());
        }
    }

    private record ValueRangeMapper<From_, To_>(CountableValueRange<From_> sourceValueRange,
            Function<From_, To_> valueConvertor, Function<To_, From_> inverseValueConvertor)
            implements
                CountableValueRange<To_> {

        @Override
        public long getSize() {
            return sourceValueRange.getSize();
        }

        @Override
        public To_ get(long index) {
            return valueConvertor.apply(sourceValueRange.get(index));
        }

        @Override
        public Iterator<To_> createOriginalIterator() {
            return new IteratorMapper<>(sourceValueRange.createOriginalIterator(), valueConvertor);
        }

        @Override
        public boolean isEmpty() {
            return sourceValueRange.isEmpty();
        }

        @Override
        public boolean contains(To_ value) {
            return sourceValueRange.contains(inverseValueConvertor.apply(value));
        }

        @Override
        public Iterator<To_> createRandomIterator(Random random) {
            return new IteratorMapper<>(sourceValueRange.createRandomIterator(random), valueConvertor);
        }
    }

    public static CountableValueRange<PythonInteger> createIntValueRange(BigInteger from, BigInteger to) {
        return new ValueRangeMapper<>(ValueRangeFactory.createBigIntegerValueRange(from, to),
                PythonInteger::valueOf,
                pythonInteger -> pythonInteger.value);
    }

    public static CountableValueRange<PythonInteger> createIntValueRange(BigInteger from, BigInteger to, BigInteger step) {
        return new ValueRangeMapper<>(ValueRangeFactory.createBigIntegerValueRange(from, to, step),
                PythonInteger::valueOf,
                pythonInteger -> pythonInteger.value);
    }

    public static CountableValueRange<PythonFloat> createFloatValueRange(BigDecimal from, BigDecimal to) {
        return new ValueRangeMapper<>(ValueRangeFactory.createBigDecimalValueRange(from, to),
                decimal -> PythonFloat.valueOf(decimal.doubleValue()),
                pythonFloat -> BigDecimal.valueOf(pythonFloat.value));
    }

    public static CountableValueRange<PythonFloat> createFloatValueRange(BigDecimal from, BigDecimal to, BigDecimal step) {
        return new ValueRangeMapper<>(ValueRangeFactory.createBigDecimalValueRange(from, to, step),
                decimal -> PythonFloat.valueOf(decimal.doubleValue()),
                pythonFloat -> BigDecimal.valueOf(pythonFloat.value));
    }

    public static CountableValueRange<PythonBoolean> createBooleanValueRange() {
        return new ValueRangeMapper<>(ValueRangeFactory.createBooleanValueRange(),
                PythonBoolean::valueOf,
                PythonBoolean::getBooleanValue);
    }
}
