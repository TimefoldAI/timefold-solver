package ai.timefold.jpyinterpreter.types;

import java.util.Map;
import java.util.Objects;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonSlice extends AbstractPythonLikeObject {
    public static PythonLikeType SLICE_TYPE = new PythonLikeType("slice", PythonSlice.class);
    public static PythonLikeType $TYPE = SLICE_TYPE;

    public final PythonLikeObject start;
    public final PythonLikeObject stop;
    public final PythonLikeObject step;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonSlice::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        SLICE_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
            PythonLikeObject start;
            PythonLikeObject stop;
            PythonLikeObject step;

            namedArguments = (namedArguments != null) ? namedArguments : Map.of();

            if (positionalArguments.size() == 3) {
                start = positionalArguments.get(0);
                stop = positionalArguments.get(1);
                step = positionalArguments.get(2);
            } else if (positionalArguments.size() == 2) {
                start = positionalArguments.get(0);
                stop = positionalArguments.get(1);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonNone.INSTANCE);
            } else if (positionalArguments.size() == 1 && namedArguments.containsKey(PythonString.valueOf("stop"))) {
                start = positionalArguments.get(0);
                stop = namedArguments.getOrDefault(PythonString.valueOf("stop"), PythonNone.INSTANCE);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonNone.INSTANCE);
            } else if (positionalArguments.size() == 1) {
                stop = positionalArguments.get(0);
                start = PythonInteger.valueOf(0);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonNone.INSTANCE);
            } else if (positionalArguments.isEmpty()) {
                start = namedArguments.getOrDefault(PythonString.valueOf("start"), PythonInteger.valueOf(0));
                stop = namedArguments.getOrDefault(PythonString.valueOf("stop"), PythonNone.INSTANCE);
                step = namedArguments.getOrDefault(PythonString.valueOf("step"), PythonNone.INSTANCE);
            } else {
                throw new ValueError("slice expects 1 to 3 arguments, got " + positionalArguments.size());
            }

            return new PythonSlice(start, stop, step);
        }));

        // Unary
        SLICE_TYPE.addUnaryMethod(PythonUnaryOperator.HASH, PythonSlice.class.getMethod("pythonHash"));

        // Binary
        SLICE_TYPE.addBinaryMethod(PythonBinaryOperator.EQUAL, PythonSlice.class.getMethod("pythonEquals", PythonSlice.class));

        // Other methods
        SLICE_TYPE.addMethod("indices", PythonSlice.class.getMethod("indices", PythonInteger.class));

        return SLICE_TYPE;
    }

    public PythonSlice(PythonLikeObject start, PythonLikeObject stop, PythonLikeObject step) {
        super(SLICE_TYPE);
        this.start = start;
        this.stop = stop;
        this.step = step;

        $setAttribute("start", (start != null) ? start : PythonNone.INSTANCE);
        $setAttribute("stop", (stop != null) ? stop : PythonNone.INSTANCE);
        $setAttribute("step", (step != null) ? step : PythonNone.INSTANCE);
    }

    /**
     * Convert index into a index for a sequence of length {@code length}. May be outside the range
     * [0, length - 1]. Use for indexing into a sequence.
     *
     * @param index The given index
     * @param length The length
     * @return index, if index in [0, length -1]; length - index, if index &lt; 0.
     */
    public static int asIntIndexForLength(PythonInteger index, int length) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            return length + indexAsInt;
        } else {
            return indexAsInt;
        }
    }

    /**
     * Convert index into a VALID start index for a sequence of length {@code length}. bounding it to the
     * range [0, length - 1]. Use for sequence operations that need to search an portion of a sequence.
     *
     * @param index The given index
     * @param length The length
     * @return index, if index in [0, length -1]; length - index, if index &lt; 0 and -index &le; length;
     *         otherwise 0 (if the index represent a position before 0) or length - 1 (if the index represent a
     *         position after the sequence).
     */
    public static int asValidStartIntIndexForLength(PythonInteger index, int length) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            return Math.max(0, Math.min(length - 1, length + indexAsInt));
        } else {
            return Math.max(0, Math.min(length - 1, indexAsInt));
        }
    }

    /**
     * Convert index into a VALID end index for a sequence of length {@code length}. bounding it to the
     * range [0, length]. Use for sequence operations that need to search an portion of a sequence.
     *
     * @param index The given index
     * @param length The length
     * @return index, if index in [0, length]; length - index, if index &lt; 0 and -index &le; length + 1;
     *         otherwise 0 (if the index represent a position before 0) or length (if the index represent a
     *         position after the sequence).
     */
    public static int asValidEndIntIndexForLength(PythonInteger index, int length) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            return Math.max(0, Math.min(length, length + indexAsInt));
        } else {
            return Math.max(0, Math.min(length, indexAsInt));
        }
    }

    private record SliceIndices(int start, int stop, int strideLength) {
    }

    private SliceIndices getSliceIndices(int length) {
        return new SliceIndices(getStartIndex(length), getStopIndex(length), getStrideLength());
    }

    private SliceIndices getSliceIndices(PythonInteger length) {
        return getSliceIndices(length.getValue().intValue());
    }

    public PythonLikeTuple indices(PythonInteger sequenceLength) {
        var sliceIndices = getSliceIndices(sequenceLength);

        return PythonLikeTuple.fromItems(PythonInteger.valueOf(sliceIndices.start),
                PythonInteger.valueOf(sliceIndices.start),
                PythonInteger.valueOf(sliceIndices.strideLength));
    }

    public int getStartIndex(int length) {
        int startIndex;
        boolean isReversed = getStrideLength() < 0;

        if (start instanceof PythonInteger) {
            startIndex = ((PythonInteger) start).value.intValueExact();
        } else if (start == PythonNone.INSTANCE) {
            startIndex = isReversed ? length - 1 : 0;
        } else {
            startIndex = ((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(start)).value.intValueExact();
        }

        if (startIndex < 0) {
            startIndex = length + startIndex;
        }

        if (!isReversed && startIndex > length) {
            startIndex = length;
        } else if (isReversed && startIndex > length - 1) {
            startIndex = length - 1;
        }

        return startIndex;
    }

    public int getStopIndex(int length) {
        int stopIndex;
        boolean isReversed = getStrideLength() < 0;

        if (stop instanceof PythonInteger) {
            stopIndex = ((PythonInteger) stop).value.intValueExact();
        } else if (stop == PythonNone.INSTANCE) {
            stopIndex = isReversed ? -length - 1 : length; // use -length - 1 so length - stopIndex = -1
        } else {
            stopIndex = ((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(stop)).value.intValueExact();
        }

        if (stopIndex < 0) {
            stopIndex = length + stopIndex;
        }

        if (!isReversed && stopIndex > length) {
            stopIndex = length;
        } else if (isReversed && stopIndex > length - 1) {
            stopIndex = length - 1;
        }

        return stopIndex;
    }

    public int getStrideLength() {
        PythonInteger strideLength;

        if (step instanceof PythonInteger) {
            strideLength = (PythonInteger) step;
        } else if (step != null && step != PythonNone.INSTANCE) {
            strideLength = (PythonInteger) UnaryDunderBuiltin.INDEX.invoke(step);
        } else {
            strideLength = PythonInteger.ONE;
        }

        int out = strideLength.value.intValueExact();

        if (out == 0) {
            throw new ValueError("stride length cannot be zero");
        }

        return out;
    }

    public void iterate(int length, SliceConsumer consumer) {
        var sliceIndices = getSliceIndices(length);

        int step = 0;
        if (sliceIndices.strideLength < 0) {
            for (int i = sliceIndices.start; i > sliceIndices.stop; i += sliceIndices.strideLength) {
                consumer.accept(i, step);
                step++;
            }
        } else {
            for (int i = sliceIndices.start; i < sliceIndices.stop; i += sliceIndices.strideLength) {
                consumer.accept(i, step);
                step++;
            }
        }
    }

    private boolean isReversed() {
        return getStrideLength() < 0;
    }

    public int getSliceSize(int length) {
        var sliceIndices = getSliceIndices(length);
        int span = sliceIndices.stop - sliceIndices.start;
        int strideLength = sliceIndices.strideLength;

        // ceil division
        return span / strideLength + (span % strideLength == 0 ? 0 : 1);
    }

    public PythonBoolean pythonEquals(PythonSlice other) {
        return PythonBoolean.valueOf(this.equals(other));
    }

    public PythonInteger pythonHash() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonSlice that = (PythonSlice) o;
        return Objects.equals(start, that.start) && Objects.equals(stop, that.stop) && Objects.equals(step, that.step);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, stop, step);
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    public interface SliceConsumer {
        void accept(int index, int step);
    }
}
