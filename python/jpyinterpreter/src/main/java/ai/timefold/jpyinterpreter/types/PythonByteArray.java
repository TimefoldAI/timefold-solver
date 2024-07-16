package ai.timefold.jpyinterpreter.types;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonTernaryOperator;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.collections.DelegatePythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeList;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.errors.lookup.IndexError;
import ai.timefold.jpyinterpreter.types.errors.unicode.UnicodeDecodeError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.util.ByteCharSequence;
import ai.timefold.jpyinterpreter.util.StringFormatter;

public class PythonByteArray extends AbstractPythonLikeObject implements PythonBytesLikeObject {
    private static final PythonByteArray ASCII_SPACE = new PythonByteArray(new byte[] { ' ' });
    private static final BitSet ASCII_WHITESPACE_BITSET = asBitSet(new PythonByteArray(
            new byte[] { ' ', '\t', '\n', '\r', 0x0b, '\f' }));

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonByteArray::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        BuiltinTypes.BYTE_ARRAY_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
            if (positionalArguments.isEmpty()) {
                return new PythonByteArray();
            } else if (positionalArguments.size() == 1) {
                PythonLikeObject arg = positionalArguments.get(0);
                if (arg instanceof PythonInteger) {
                    return new PythonByteArray(new byte[((PythonInteger) arg).value.intValueExact()]);
                } else {
                    PythonIterator<?> iterator = (PythonIterator<?>) UnaryDunderBuiltin.ITERATOR.invoke(arg);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] toWrite = new byte[1];
                    while (iterator.hasNext()) {
                        PythonLikeObject item = iterator.nextPythonItem();
                        if (!(item instanceof PythonInteger)) {
                            throw new ValueError("bytearray argument 1 must be an int or an iterable of int");
                        }
                        toWrite[0] = ((PythonInteger) item).asByte();
                        out.writeBytes(toWrite);
                    }
                    return new PythonByteArray(out.toByteArray());
                }
            } else {
                throw new ValueError("bytearray takes 0 or 1 arguments, not " + positionalArguments.size());
            }
        }));

        // Unary
        BuiltinTypes.BYTE_ARRAY_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                PythonByteArray.class.getMethod("repr"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING, PythonByteArray.class.getMethod("asString"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR,
                PythonByteArray.class.getMethod("getIterator"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, PythonByteArray.class.getMethod("getLength"));

        // Binary
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonByteArray.class.getMethod("getCharAt", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonByteArray.class.getMethod("getSubsequence", PythonSlice.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.DELETE_ITEM,
                PythonByteArray.class.getMethod("deleteIndex", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.DELETE_ITEM,
                PythonByteArray.class.getMethod("deleteSlice", PythonSlice.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.CONTAINS,
                PythonByteArray.class.getMethod("containsSubsequence", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.ADD,
                PythonByteArray.class.getMethod("concat", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.INPLACE_ADD,
                PythonByteArray.class.getMethod("inplaceAdd", PythonLikeObject.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.MULTIPLY,
                PythonByteArray.class.getMethod("repeat", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.INPLACE_MULTIPLY,
                PythonByteArray.class.getMethod("inplaceRepeat", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonByteArray.class.getMethod("interpolate", PythonLikeObject.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonByteArray.class.getMethod("interpolate", PythonLikeTuple.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonByteArray.class.getMethod("interpolate", PythonLikeDict.class));

        // Ternary
        BuiltinTypes.BYTE_ARRAY_TYPE.addTernaryMethod(PythonTernaryOperator.SET_ITEM,
                PythonByteArray.class.getMethod("setByte", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addTernaryMethod(PythonTernaryOperator.SET_ITEM,
                PythonByteArray.class.getMethod("setSlice", PythonSlice.class, PythonLikeObject.class));

        // Other
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("append", PythonByteArray.class.getMethod("appendByte", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("clear", PythonByteArray.class.getMethod("clear"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("copy", PythonByteArray.class.getMethod("copy"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("extend", PythonByteArray.class.getMethod("extend", PythonLikeObject.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("insert",
                PythonByteArray.class.getMethod("insert", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("pop", PythonByteArray.class.getMethod("pop"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("pop", PythonByteArray.class.getMethod("pop", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("remove", PythonByteArray.class.getMethod("remove", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("reverse", PythonByteArray.class.getMethod("reverse"));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("capitalize", PythonByteArray.class.getMethod("capitalize"));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("center", PythonByteArray.class.getMethod("center", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("center",
                PythonByteArray.class.getMethod("center", PythonInteger.class, PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("count", PythonByteArray.class.getMethod("count", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("count",
                PythonByteArray.class.getMethod("count", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("count",
                PythonByteArray.class.getMethod("count", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("count", PythonByteArray.class.getMethod("count", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("count",
                PythonByteArray.class.getMethod("count", PythonByteArray.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("count",
                PythonByteArray.class.getMethod("count", PythonByteArray.class, PythonInteger.class, PythonInteger.class));

        // TODO: decode

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("endswith", PythonByteArray.class.getMethod("endsWith", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("endswith", PythonByteArray.class.getMethod("endsWith", PythonLikeTuple.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("endswith",
                PythonByteArray.class.getMethod("endsWith", PythonByteArray.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("endswith",
                PythonByteArray.class.getMethod("endsWith", PythonLikeTuple.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("endswith",
                PythonByteArray.class.getMethod("endsWith", PythonByteArray.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("endswith",
                PythonByteArray.class.getMethod("endsWith", PythonLikeTuple.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("expandtabs", PythonByteArray.class.getMethod("expandTabs"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("expandtabs",
                PythonByteArray.class.getMethod("expandTabs", PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("find", PythonByteArray.class.getMethod("find", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("find", PythonByteArray.class.getMethod("find", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("find",
                PythonByteArray.class.getMethod("find", PythonByteArray.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("find",
                PythonByteArray.class.getMethod("find", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("find",
                PythonByteArray.class.getMethod("find", PythonByteArray.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("find",
                PythonByteArray.class.getMethod("find", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("index", PythonByteArray.class.getMethod("index", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("index", PythonByteArray.class.getMethod("index", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("index",
                PythonByteArray.class.getMethod("index", PythonByteArray.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("index",
                PythonByteArray.class.getMethod("index", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("index",
                PythonByteArray.class.getMethod("index", PythonByteArray.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("index",
                PythonByteArray.class.getMethod("index", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("isalnum", PythonByteArray.class.getMethod("isAlphaNumeric"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("isalpha", PythonByteArray.class.getMethod("isAlpha"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("isascii", PythonByteArray.class.getMethod("isAscii"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("isdigit", PythonByteArray.class.getMethod("isDigit"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("islower", PythonByteArray.class.getMethod("isLower"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("isspace", PythonByteArray.class.getMethod("isSpace"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("istitle", PythonByteArray.class.getMethod("isTitle"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("isupper", PythonByteArray.class.getMethod("isUpper"));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("join", PythonByteArray.class.getMethod("join", PythonLikeObject.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("ljust", PythonByteArray.class.getMethod("leftJustify", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("ljust",
                PythonByteArray.class.getMethod("leftJustify", PythonInteger.class, PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("lower", PythonByteArray.class.getMethod("lower"));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("lstrip", PythonByteArray.class.getMethod("leftStrip"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("lstrip", PythonByteArray.class.getMethod("leftStrip", PythonNone.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("lstrip", PythonByteArray.class.getMethod("leftStrip", PythonByteArray.class));

        // TODO: maketrans

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("partition",
                PythonByteArray.class.getMethod("partition", PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("removeprefix",
                PythonByteArray.class.getMethod("removePrefix", PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("removesuffix",
                PythonByteArray.class.getMethod("removeSuffix", PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("replace",
                PythonByteArray.class.getMethod("replace", PythonByteArray.class, PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("replace",
                PythonByteArray.class.getMethod("replace", PythonByteArray.class, PythonByteArray.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rfind", PythonByteArray.class.getMethod("rightFind", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rfind", PythonByteArray.class.getMethod("rightFind", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rfind",
                PythonByteArray.class.getMethod("rightFind", PythonByteArray.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rfind",
                PythonByteArray.class.getMethod("rightFind", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rfind",
                PythonByteArray.class.getMethod("rightFind", PythonByteArray.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rfind",
                PythonByteArray.class.getMethod("rightFind", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rindex", PythonByteArray.class.getMethod("rightIndex", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rindex", PythonByteArray.class.getMethod("rightIndex", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rindex",
                PythonByteArray.class.getMethod("rightIndex", PythonByteArray.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rindex",
                PythonByteArray.class.getMethod("rightIndex", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rindex",
                PythonByteArray.class.getMethod("rightIndex", PythonByteArray.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rindex",
                PythonByteArray.class.getMethod("rightIndex", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rjust", PythonByteArray.class.getMethod("rightJustify", PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rjust",
                PythonByteArray.class.getMethod("rightJustify", PythonInteger.class, PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rpartition",
                PythonByteArray.class.getMethod("rightPartition", PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rsplit", PythonByteArray.class.getMethod("rightSplit"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rsplit", PythonByteArray.class.getMethod("rightSplit", PythonNone.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rsplit", PythonByteArray.class.getMethod("rightSplit", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rsplit",
                PythonByteArray.class.getMethod("rightSplit", PythonNone.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rsplit",
                PythonByteArray.class.getMethod("rightSplit", PythonByteArray.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rstrip", PythonByteArray.class.getMethod("rightStrip"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rstrip", PythonByteArray.class.getMethod("rightStrip", PythonNone.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("rstrip", PythonByteArray.class.getMethod("rightStrip", PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("split", PythonByteArray.class.getMethod("split"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("split", PythonByteArray.class.getMethod("split", PythonNone.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("split", PythonByteArray.class.getMethod("split", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("split",
                PythonByteArray.class.getMethod("split", PythonNone.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("split",
                PythonByteArray.class.getMethod("split", PythonByteArray.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("splitlines", PythonByteArray.class.getMethod("splitLines"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("splitlines",
                PythonByteArray.class.getMethod("splitLines", PythonBoolean.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("startswith",
                PythonByteArray.class.getMethod("startsWith", PythonByteArray.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("startswith",
                PythonByteArray.class.getMethod("startsWith", PythonLikeTuple.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("startswith",
                PythonByteArray.class.getMethod("startsWith", PythonByteArray.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("startswith",
                PythonByteArray.class.getMethod("startsWith", PythonLikeTuple.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("startswith",
                PythonByteArray.class.getMethod("startsWith", PythonByteArray.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("startswith",
                PythonByteArray.class.getMethod("startsWith", PythonLikeTuple.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("strip", PythonByteArray.class.getMethod("strip"));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("strip", PythonByteArray.class.getMethod("strip", PythonNone.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("strip", PythonByteArray.class.getMethod("strip", PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("swapcase", PythonByteArray.class.getMethod("swapCase"));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("title", PythonByteArray.class.getMethod("title"));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("translate", PythonByteArray.class.getMethod("translate", PythonBytes.class));
        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("translate",
                PythonByteArray.class.getMethod("translate", PythonByteArray.class));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("upper", PythonByteArray.class.getMethod("upper"));

        BuiltinTypes.BYTE_ARRAY_TYPE.addMethod("zfill", PythonByteArray.class.getMethod("zfill", PythonInteger.class));

        return BuiltinTypes.BYTE_ARRAY_TYPE;
    }

    public ByteBuffer valueBuffer;

    public PythonByteArray() {
        super(BuiltinTypes.BYTE_ARRAY_TYPE);
        this.valueBuffer = ByteBuffer.allocate(4096);
        valueBuffer.limit(0);
    }

    public PythonByteArray(byte[] data) {
        super(BuiltinTypes.BYTE_ARRAY_TYPE);
        this.valueBuffer = ByteBuffer.allocate(data.length);
        valueBuffer.put(data);
        valueBuffer.position(0);
    }

    public PythonByteArray(ByteBuffer valueBuffer) {
        super(BuiltinTypes.BYTE_ARRAY_TYPE);
        this.valueBuffer = valueBuffer;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        return valueBuffer.duplicate().asReadOnlyBuffer();
    }

    public final ByteCharSequence asCharSequence() {
        return new ByteCharSequence(valueBuffer.array(), 0, valueBuffer.limit());
    }

    public final PythonString asAsciiString() {
        return PythonString.valueOf(asCharSequence().toString());
    }

    public static PythonByteArray fromIntTuple(PythonLikeTuple tuple) {
        byte[] out = new byte[tuple.size()];
        IntStream.range(0, tuple.size()).forEach(index -> out[index] = ((PythonInteger) tuple.get(index)).asByte());
        return new PythonByteArray(out);
    }

    public final PythonLikeTuple asIntTuple() {
        return IntStream.range(0, valueBuffer.limit())
                .mapToObj(index -> PythonBytes.BYTE_TO_INT[Byte.toUnsignedInt(valueBuffer.get(index))])
                .collect(Collectors.toCollection(PythonLikeTuple::new));
    }

    private static BitSet asBitSet(PythonByteArray bytesInBitSet) {
        BitSet out = new BitSet();
        for (int i = 0; i < bytesInBitSet.valueBuffer.limit(); i++) {
            out.set(bytesInBitSet.valueBuffer.get(i) & 0xFF);
        }
        return out;
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(valueBuffer.limit());
    }

    public PythonInteger getCharAt(PythonInteger position) {
        int index = PythonSlice.asIntIndexForLength(position, valueBuffer.limit());

        if (index >= valueBuffer.limit()) {
            throw new IndexError("position " + position + " larger than bytes length " + valueBuffer.limit());
        } else if (index < 0) {
            throw new IndexError("position " + position + " is less than 0");
        }

        return PythonBytes.BYTE_TO_INT[Byte.toUnsignedInt(valueBuffer.get(index))];
    }

    public PythonByteArray getSubsequence(PythonSlice slice) {
        int length = valueBuffer.limit();
        int start = slice.getStartIndex(length);
        int stop = slice.getStopIndex(length);
        int step = slice.getStrideLength();

        if (step == 1) {
            if (stop <= start) {
                return new PythonByteArray(new byte[] {});
            } else {
                return new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), start, stop));
            }
        } else {
            byte[] out = new byte[slice.getSliceSize(length)];
            slice.iterate(length, (index, iteration) -> {
                out[iteration] = valueBuffer.get(index);
            });
            return new PythonByteArray(out);
        }
    }

    public PythonBoolean containsSubsequence(PythonByteArray subsequence) {
        if (subsequence.valueBuffer.limit() == 0) {
            return PythonBoolean.TRUE;
        }

        if (subsequence.valueBuffer.limit() > valueBuffer.limit()) {
            return PythonBoolean.FALSE;
        }

        for (int i = 0; i <= valueBuffer.limit() - subsequence.valueBuffer.limit(); i++) {
            if (Arrays.equals(valueBuffer.array(), i, i + subsequence.valueBuffer.limit(),
                    subsequence.valueBuffer.array(), 0, subsequence.valueBuffer.limit())) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonByteArray concat(PythonByteArray other) {
        if (valueBuffer.limit() == 0) {
            return new PythonByteArray(other.valueBuffer.duplicate());
        } else if (other.valueBuffer.limit() == 0) {
            return new PythonByteArray(valueBuffer.duplicate());
        } else {
            byte[] out = new byte[valueBuffer.limit() + other.valueBuffer.limit()];
            System.arraycopy(valueBuffer.array(), 0, out, 0, valueBuffer.limit());
            System.arraycopy(other.valueBuffer.array(), 0, out, valueBuffer.limit(), other.valueBuffer.limit());
            return new PythonByteArray(out);
        }
    }

    public PythonByteArray repeat(PythonInteger times) {
        int timesAsInt = times.value.intValueExact();

        if (timesAsInt <= 0) {
            return new PythonByteArray(new byte[] {});
        }

        byte[] out = new byte[valueBuffer.limit() * timesAsInt];
        for (int i = 0; i < timesAsInt; i++) {
            System.arraycopy(valueBuffer.array(), 0, out, i * valueBuffer.limit(), valueBuffer.limit());
        }
        return new PythonByteArray(out);
    }

    public DelegatePythonIterator<PythonInteger> getIterator() {
        return new DelegatePythonIterator<>(IntStream.range(0, valueBuffer.limit())
                .mapToObj(index -> PythonBytes.BYTE_TO_INT[Byte.toUnsignedInt(valueBuffer.get(index))])
                .iterator());
    }

    public PythonInteger countByte(byte query, int start, int end) {
        int count = 0;
        for (int i = start; i < end; i++) {
            if (valueBuffer.get(i) == query) {
                count++;
            }
        }
        return PythonInteger.valueOf(count);
    }

    public PythonInteger count(PythonInteger byteAsInt) {
        byte query = byteAsInt.asByte();
        return countByte(query, 0, valueBuffer.limit());
    }

    public PythonInteger count(PythonInteger byteAsInt, PythonInteger start) {
        byte query = byteAsInt.asByte();
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());

        return countByte(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger count(PythonInteger byteAsInt, PythonInteger start, PythonInteger end) {
        byte query = byteAsInt.asByte();
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return countByte(query, startAsInt, endAsInt);
    }

    private PythonInteger countSubsequence(byte[] query, int from, int to) {
        int count = 0;

        if ((to - from) == 0 || query.length > (to - from)) {
            return PythonInteger.ZERO;
        }

        if (query.length == 0) {
            return PythonInteger.valueOf((to - from) + 1);
        }

        for (int i = from; i <= to - query.length; i++) {
            if (Arrays.equals(valueBuffer.array(), i, i + query.length,
                    query, 0, query.length)) {
                count++;
                i += query.length - 1;
            }
        }
        return PythonInteger.valueOf(count);
    }

    public PythonInteger count(PythonByteArray bytes) {
        return countSubsequence(bytes.valueBuffer.array(), 0, valueBuffer.limit());
    }

    public PythonInteger count(PythonByteArray bytes, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());

        return countSubsequence(bytes.valueBuffer.array(), startAsInt, valueBuffer.limit());
    }

    public PythonInteger count(PythonByteArray bytes, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return countSubsequence(bytes.valueBuffer.array(), startAsInt, endAsInt);
    }

    private static byte[] getBytes(PythonLikeObject iterable) {
        if (iterable instanceof PythonBytes) {
            return ((PythonBytes) iterable).value;
        } else if (iterable instanceof PythonByteArray) {
            return ((PythonByteArray) iterable).asByteArray();
        } else {
            PythonIterator<?> iterator = (PythonIterator<?>) UnaryDunderBuiltin.ITERATOR.invoke(iterable);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (iterator.hasNext()) {
                PythonLikeObject next = iterator.nextPythonItem();
                byte[] byteWrapper = new byte[1];
                if (!(next instanceof PythonInteger)) {
                    throw new TypeError("'" + next.$getType().getTypeName() + "' object cannot be interpreted as an integer");
                }
                byteWrapper[0] = ((PythonInteger) next).asByte();
                out.writeBytes(byteWrapper);
            }
            return out.toByteArray();
        }
    }

    private void ensureCapacity(int minimumCapacity) {
        int oldCapacity = valueBuffer.capacity();
        if (oldCapacity >= minimumCapacity) {
            return;
        }

        int newCapacity = Math.max(oldCapacity + (oldCapacity >> 1), minimumCapacity);

        ByteBuffer newValueBuffer = ByteBuffer.allocate(newCapacity);
        System.arraycopy(valueBuffer.array(), 0, newValueBuffer.array(), 0, valueBuffer.limit());
        newValueBuffer.limit(valueBuffer.limit());
        this.valueBuffer = newValueBuffer;
    }

    private void insertExtraBytesAt(int position, int extraBytes) {
        System.arraycopy(valueBuffer.array(), position,
                valueBuffer.array(), position + extraBytes,
                valueBuffer.limit() - position);
        valueBuffer.limit(valueBuffer.limit() + extraBytes);
    }

    private void removeBytesStartingAt(int position, int removedBytes) {
        System.arraycopy(valueBuffer.array(), position + removedBytes,
                valueBuffer.array(), position,
                valueBuffer.limit() - position - removedBytes);
        valueBuffer.limit(valueBuffer.limit() - removedBytes);
    }

    public PythonNone setByte(PythonInteger index, PythonInteger item) {
        int indexAsInt = PythonSlice.asIntIndexForLength(index, valueBuffer.limit());
        if (indexAsInt < 0 || indexAsInt >= valueBuffer.limit()) {
            throw new IndexError("bytearray index out of range");
        }
        valueBuffer.put(indexAsInt, item.asByte());
        return PythonNone.INSTANCE;
    }

    public PythonNone setSlice(PythonSlice slice, PythonLikeObject iterable) {
        byte[] iterableBytes = getBytes(iterable);
        if (slice.getStrideLength() == 1) {
            int sizeDifference = iterableBytes.length - slice.getSliceSize(valueBuffer.limit());
            if (sizeDifference == 0) {
                valueBuffer.position(slice.getStartIndex(valueBuffer.limit()));
                valueBuffer.put(iterableBytes);
            } else if (sizeDifference > 0) {
                // inserted extra bytes
                int oldLimit = valueBuffer.limit();
                ensureCapacity(valueBuffer.capacity() + sizeDifference);
                insertExtraBytesAt(slice.getStopIndex(oldLimit), sizeDifference);
                System.arraycopy(iterableBytes, 0, valueBuffer.array(), slice.getStartIndex(oldLimit),
                        iterableBytes.length);
            } else {
                // removed some bytes
                int oldLimit = valueBuffer.limit();
                removeBytesStartingAt(slice.getStopIndex(oldLimit) + sizeDifference, -sizeDifference);
                System.arraycopy(iterableBytes, 0, valueBuffer.array(), slice.getStartIndex(oldLimit),
                        iterableBytes.length);
            }
        } else {
            if (iterableBytes.length == 0) {
                deleteSlice(slice);
            } else {
                if (iterableBytes.length != slice.getSliceSize(valueBuffer.limit())) {
                    throw new ValueError(
                            "attempt to assign bytes of size " + iterableBytes.length + " to extended slice of size " +
                                    slice.getSliceSize(valueBuffer.limit()));
                }

                slice.iterate(valueBuffer.limit(), (index, step) -> {
                    valueBuffer.put(index, iterableBytes[step]);
                });
            }
        }
        return PythonNone.INSTANCE;
    }

    public PythonNone deleteIndex(PythonInteger index) {
        int indexAsInt = PythonSlice.asIntIndexForLength(index, valueBuffer.limit());
        removeBytesStartingAt(indexAsInt, 1);
        return PythonNone.INSTANCE;
    }

    public PythonNone deleteSlice(PythonSlice deletedSlice) {
        if (deletedSlice.getStrideLength() == 1) {
            removeBytesStartingAt(deletedSlice.getStartIndex(valueBuffer.limit()),
                    deletedSlice.getSliceSize(valueBuffer.limit()));
        } else {
            if (deletedSlice.getStrideLength() > 0) {
                deletedSlice.iterate(valueBuffer.limit(), (index, step) -> {
                    removeBytesStartingAt(index - step, 1);
                });
            } else {
                deletedSlice.iterate(valueBuffer.limit(), (index, step) -> {
                    removeBytesStartingAt(index, 1);
                });
            }
        }
        return PythonNone.INSTANCE;
    }

    public PythonNone appendByte(PythonInteger addedByte) {
        byte toAdd = addedByte.asByte();
        ensureCapacity(valueBuffer.limit() + 1);
        valueBuffer.limit(valueBuffer.limit() + 1);
        valueBuffer.position(valueBuffer.limit() - 1);
        valueBuffer.put(toAdd);
        return PythonNone.INSTANCE;
    }

    public PythonNone clear() {
        valueBuffer.limit(0);
        return PythonNone.INSTANCE;
    }

    public PythonByteArray copy() {
        byte[] copiedData = asByteArray();
        return new PythonByteArray(copiedData);
    }

    public PythonNone extend(PythonLikeObject iterable) {
        byte[] data = getBytes(iterable);
        ensureCapacity(valueBuffer.limit() + data.length);
        int oldLimit = valueBuffer.limit();
        valueBuffer.limit(valueBuffer.limit() + data.length);
        valueBuffer.position(oldLimit);
        valueBuffer.put(data);
        return PythonNone.INSTANCE;
    }

    public PythonByteArray inplaceAdd(PythonLikeObject iterable) {
        extend(iterable);
        return this;
    }

    public PythonByteArray inplaceRepeat(PythonLikeObject indexable) {
        return inplaceRepeat((PythonInteger) UnaryDunderBuiltin.INDEX.invoke(indexable));
    }

    public PythonByteArray inplaceRepeat(PythonInteger index) {
        int indexAsInt = index.value.intValueExact();
        if (indexAsInt <= 0) {
            clear();
            return this;
        } else if (indexAsInt == 1) {
            return this;
        } else {
            ensureCapacity(valueBuffer.limit() * indexAsInt);
            int oldLimit = valueBuffer.limit();
            valueBuffer.limit(oldLimit * indexAsInt);
            for (int i = 1; i < indexAsInt; i++) {
                System.arraycopy(valueBuffer.array(), 0, valueBuffer.array(), i * oldLimit, oldLimit);
            }
            return this;
        }
    }

    public PythonNone insert(PythonInteger index, PythonInteger value) {
        byte toInsert = value.asByte();
        ensureCapacity(valueBuffer.limit() + 1);
        int indexAsInt = PythonSlice.asIntIndexForLength(index, valueBuffer.limit());

        if (indexAsInt < 0) {
            indexAsInt = 0;
        }

        if (indexAsInt > valueBuffer.limit()) {
            indexAsInt = valueBuffer.limit();
        }

        insertExtraBytesAt(indexAsInt, 1);
        valueBuffer.position(indexAsInt);
        valueBuffer.put(toInsert);
        return PythonNone.INSTANCE;
    }

    public PythonInteger pop() {
        if (valueBuffer.limit() == 0) {
            throw new IndexError("pop from empty bytearray");
        }
        PythonInteger out = PythonBytes.BYTE_TO_INT[Byte.toUnsignedInt(valueBuffer.get(valueBuffer.limit() - 1))];
        valueBuffer.limit(valueBuffer.limit() - 1);
        return out;
    }

    public PythonInteger pop(PythonInteger index) {
        int indexAsInt = PythonSlice.asIntIndexForLength(index, valueBuffer.limit());
        if (valueBuffer.limit() == 0) {
            throw new IndexError("pop from empty bytearray");
        }

        if (indexAsInt < 0 || indexAsInt > valueBuffer.limit()) {
            throw new IndexError("index out of range for bytearray");
        }
        PythonInteger out = PythonBytes.BYTE_TO_INT[Byte.toUnsignedInt(valueBuffer.get(indexAsInt))];
        removeBytesStartingAt(indexAsInt, 1);
        return out;
    }

    public PythonNone remove(PythonInteger item) {
        byte queryByte = item.asByte();

        for (int i = 0; i < valueBuffer.limit(); i++) {
            if (valueBuffer.get(i) == queryByte) {
                removeBytesStartingAt(i, 1);
                return PythonNone.INSTANCE;
            }
        }
        throw new ValueError("Subsequence not found");
    }

    public PythonNone reverse() {
        int limit = valueBuffer.limit();
        byte[] data = valueBuffer.array();
        for (int i = 0; i < limit >> 1; i++) {
            byte temp = data[i];
            data[i] = data[data.length - i - 1];
            data[data.length - i - 1] = temp;
        }

        return PythonNone.INSTANCE;
    }

    public boolean hasPrefix(ByteBuffer prefixBytes, int start, int end) {
        if (prefixBytes.limit() > end - start) {
            return false;
        }

        return Arrays.equals(valueBuffer.array(), start, start + prefixBytes.limit(),
                prefixBytes.array(), 0, prefixBytes.limit());
    }

    public boolean hasSuffix(ByteBuffer suffixBytes, int start, int end) {
        if (suffixBytes.limit() > end - start) {
            return false;
        }

        return Arrays.equals(valueBuffer.array(), end - suffixBytes.limit(), end,
                suffixBytes.array(), 0, suffixBytes.limit());
    }

    public PythonByteArray removePrefix(PythonByteArray prefix) {
        return hasPrefix(prefix.valueBuffer, 0, valueBuffer.limit())
                ? new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), prefix.valueBuffer.limit(), valueBuffer.limit()))
                : this;
    }

    public PythonByteArray removeSuffix(PythonByteArray suffix) {
        return hasSuffix(suffix.valueBuffer, 0, valueBuffer.limit())
                ? new PythonByteArray(
                        Arrays.copyOfRange(valueBuffer.array(), 0, valueBuffer.limit() - suffix.valueBuffer.limit()))
                : this;
    }

    public PythonString decode() {
        try {
            return PythonString.valueOf(StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(valueBuffer).toString());
        } catch (CharacterCodingException e) {
            throw new UnicodeDecodeError(e.getMessage());
        }

    }

    public PythonString decode(PythonString charset) {
        try {
            return PythonString.valueOf(Charset.forName(charset.value).newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(valueBuffer).toString());
        } catch (CharacterCodingException e) {
            throw new UnicodeDecodeError(e.getMessage());
        }
    }

    public PythonString decode(PythonString charset, PythonString errorActionString) {
        CodingErrorAction errorAction;
        switch (errorActionString.value) {
            case "strict":
                errorAction = CodingErrorAction.REPORT;
                break;
            case "ignore":
                errorAction = CodingErrorAction.IGNORE;
                break;
            case "replace":
                errorAction = CodingErrorAction.REPLACE;
                break;
            default:
                throw new ValueError(errorActionString.repr() + " is not a valid value for errors. Possible values are: " +
                        "\"strict\", \"ignore\", \"replace\".");
        }
        try {
            return PythonString.valueOf(Charset.forName(charset.value).newDecoder()
                    .onMalformedInput(errorAction)
                    .decode(valueBuffer).toString());
        } catch (CharacterCodingException e) {
            throw new UnicodeDecodeError(e.getMessage());
        }
    }

    public PythonBoolean endsWith(PythonByteArray suffix) {
        return PythonBoolean.valueOf(hasSuffix(suffix.valueBuffer, 0, valueBuffer.limit()));
    }

    public PythonBoolean endsWith(PythonByteArray suffix, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return PythonBoolean.valueOf(hasSuffix(suffix.valueBuffer, startAsInt, valueBuffer.limit()));
    }

    public PythonBoolean endsWith(PythonByteArray suffix, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());
        return PythonBoolean.valueOf(hasSuffix(suffix.valueBuffer, startAsInt, endAsInt));
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonByteArray> suffixes) {
        for (PythonByteArray suffix : suffixes) {
            if (hasSuffix(suffix.valueBuffer, 0, valueBuffer.limit())) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonByteArray> suffixes, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());

        for (PythonByteArray suffix : suffixes) {
            if (hasSuffix(suffix.valueBuffer, startAsInt, valueBuffer.limit())) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonByteArray> suffixes, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        for (PythonByteArray suffix : suffixes) {
            if (hasSuffix(suffix.valueBuffer, startAsInt, endAsInt)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    private PythonInteger find(PythonInteger query, int start, int end) {
        byte queryByte = query.asByte();

        for (int i = start; i < end; i++) {
            if (valueBuffer.get(i) == queryByte) {
                return PythonInteger.valueOf(i);
            }
        }

        return PythonInteger.valueOf(-1);
    }

    private PythonInteger index(PythonInteger query, int start, int end) {
        byte queryByte = query.asByte();

        for (int i = start; i < end; i++) {
            if (valueBuffer.get(i) == queryByte) {
                return PythonInteger.valueOf(i);
            }
        }

        throw new ValueError("Subsequence not found");
    }

    public PythonInteger find(PythonInteger query) {
        return find(query, 0, valueBuffer.limit());
    }

    public PythonInteger find(PythonInteger query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return find(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger find(PythonInteger query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return find(query, startAsInt, endAsInt);
    }

    public PythonInteger index(PythonInteger query) {
        return index(query, 0, valueBuffer.limit());
    }

    public PythonInteger index(PythonInteger query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return index(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger index(PythonInteger query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return index(query, startAsInt, endAsInt);
    }

    private PythonInteger find(PythonBytesLikeObject query, int start, int end) {
        byte[] queryBytes = query.asByteArray();

        if (queryBytes.length == 0) {
            return (valueBuffer.limit() > 0) ? PythonInteger.ZERO : PythonInteger.valueOf(-1);
        }

        for (int i = start; i <= end - queryBytes.length; i++) {
            if (Arrays.equals(valueBuffer.array(), i, i + queryBytes.length, queryBytes, 0, queryBytes.length)) {
                return PythonInteger.valueOf(i);
            }
        }

        return PythonInteger.valueOf(-1);
    }

    private PythonInteger index(PythonBytesLikeObject query, int start, int end) {
        byte[] queryBytes = query.asByteArray();

        if (queryBytes.length == 0) {
            if (valueBuffer.limit() > 0) {
                return PythonInteger.ZERO;
            } else {
                throw new ValueError("Subsequence not found");
            }
        }

        for (int i = start; i <= end - queryBytes.length; i++) {
            if (Arrays.equals(valueBuffer.array(), i, i + queryBytes.length, queryBytes, 0, queryBytes.length)) {
                return PythonInteger.valueOf(i);
            }
        }

        throw new ValueError("Subsequence not found");
    }

    public PythonInteger find(PythonByteArray query) {
        return find(query, 0, valueBuffer.limit());
    }

    public PythonInteger find(PythonByteArray query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return find(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger find(PythonByteArray query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return find(query, startAsInt, endAsInt);
    }

    public PythonInteger index(PythonByteArray query) {
        return index(query, 0, valueBuffer.limit());
    }

    public PythonInteger index(PythonByteArray query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return index(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger index(PythonByteArray query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return index(query, startAsInt, endAsInt);
    }

    public PythonByteArray interpolate(PythonLikeObject object) {
        if (object instanceof PythonLikeTuple) {
            return interpolate((PythonLikeTuple) object);
        } else if (object instanceof PythonLikeDict) {
            return interpolate((PythonLikeDict) object);
        } else {
            return interpolate(PythonLikeTuple.fromItems(object));
        }
    }

    public PythonByteArray interpolate(PythonLikeTuple tuple) {
        return PythonString.valueOf(StringFormatter.printfInterpolate(asCharSequence(), tuple,
                StringFormatter.PrintfStringType.BYTES)).asAsciiByteArray();
    }

    public PythonByteArray interpolate(PythonLikeDict dict) {
        return PythonString.valueOf(StringFormatter.printfInterpolate(asCharSequence(), dict,
                StringFormatter.PrintfStringType.BYTES)).asAsciiByteArray();
    }

    public PythonByteArray join(PythonLikeObject iterable) {
        PythonIterator<?> iterator = (PythonIterator<?>) UnaryDunderBuiltin.ITERATOR.invoke(iterable);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (iterator.hasNext()) {
            PythonLikeObject item = iterator.nextPythonItem();

            if (!(item instanceof PythonBytesLikeObject)) {
                throw new TypeError("type " + item.$getType() + " is not a bytes-like type");
            }

            outputStream.writeBytes(((PythonBytesLikeObject) item).asByteArray());
            if (iterator.hasNext()) {
                outputStream.write(valueBuffer.array(), 0, valueBuffer.limit());
            }
        }
        return new PythonByteArray(outputStream.toByteArray());
    }

    private PythonLikeTuple partition(PythonBytesLikeObject sep, int start, int end) {
        byte[] sepBytes = sep.asByteArray();

        for (int i = start; i < end - sepBytes.length; i++) {
            int j = 0;
            for (; j < sepBytes.length; j++) {
                if (valueBuffer.get(i + j) != sepBytes[j]) {
                    break;
                }
            }

            if (j == sepBytes.length) {
                return PythonLikeTuple.fromItems(
                        new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), 0, i)),
                        sep,
                        new PythonByteArray(
                                Arrays.copyOfRange(valueBuffer.array(), i + sepBytes.length, valueBuffer.limit())));
            }
        }

        return PythonLikeTuple.fromItems(
                this,
                new PythonByteArray(new byte[] {}),
                new PythonByteArray(new byte[] {}));
    }

    public PythonLikeTuple partition(PythonByteArray sep) {
        return partition(sep, 0, valueBuffer.limit());
    }

    public PythonLikeTuple partition(PythonByteArray sep, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());

        return partition(sep, startAsInt, valueBuffer.limit());
    }

    public PythonLikeTuple partition(PythonByteArray sep, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidStartIntIndexForLength(end, valueBuffer.limit());

        return partition(sep, startAsInt, endAsInt);
    }

    public PythonByteArray replace(PythonBytesLikeObject old, PythonBytesLikeObject replacement) {
        byte[] oldBytes = old.asByteArray();
        byte[] replacementBytes = replacement.asByteArray();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int lastReplacementEnd = 0;
        for (int i = 0; i < valueBuffer.limit() - oldBytes.length; i++) {
            if (!Arrays.equals(valueBuffer.array(), i, i + oldBytes.length,
                    oldBytes, 0, oldBytes.length)) {
                continue;
            }

            outputStream.write(valueBuffer.array(), lastReplacementEnd, i - lastReplacementEnd);
            outputStream.writeBytes(replacementBytes);

            i += oldBytes.length;
            lastReplacementEnd = i;
        }

        outputStream.write(valueBuffer.array(), lastReplacementEnd, valueBuffer.limit() - lastReplacementEnd);
        return new PythonByteArray(outputStream.toByteArray());
    }

    public PythonByteArray replace(PythonBytesLikeObject old, PythonBytesLikeObject replacement, BigInteger count) {
        byte[] oldBytes = old.asByteArray();
        byte[] replacementBytes = replacement.asByteArray();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int lastReplacementEnd = 0;
        for (int i = 0; i < valueBuffer.limit() - oldBytes.length; i++) {
            if (count.compareTo(BigInteger.ZERO) == 0) {
                break;
            }

            if (!Arrays.equals(valueBuffer.array(), i, i + oldBytes.length,
                    oldBytes, 0, oldBytes.length)) {
                continue;
            }

            outputStream.write(valueBuffer.array(), lastReplacementEnd, i - lastReplacementEnd);
            outputStream.writeBytes(replacementBytes);

            i += oldBytes.length;
            lastReplacementEnd = i;
            count = count.subtract(BigInteger.ONE);
        }

        outputStream.write(valueBuffer.array(), lastReplacementEnd, valueBuffer.limit() - lastReplacementEnd);
        return new PythonByteArray(outputStream.toByteArray());
    }

    public PythonByteArray replace(PythonByteArray old, PythonByteArray replacement) {
        return replace((PythonBytesLikeObject) old, replacement);
    }

    public PythonByteArray replace(PythonByteArray old, PythonByteArray replacement, PythonInteger count) {
        return replace(old, replacement, count.value);
    }

    private PythonInteger rightFind(PythonInteger query, int start, int end) {
        byte queryByte = query.asByte();

        for (int i = end - 1; i >= start; i--) {
            if (valueBuffer.get(i) == queryByte) {
                return PythonInteger.valueOf(i);
            }
        }

        return PythonInteger.valueOf(-1);
    }

    private PythonInteger rightIndex(PythonInteger query, int start, int end) {
        byte queryByte = query.asByte();

        for (int i = end - 1; i >= start; i--) {
            if (valueBuffer.get(i) == queryByte) {
                return PythonInteger.valueOf(i);
            }
        }

        throw new ValueError("Subsequence not found");
    }

    public PythonInteger rightFind(PythonInteger query) {
        return rightFind(query, 0, valueBuffer.limit());
    }

    public PythonInteger rightFind(PythonInteger query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return rightFind(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger rightFind(PythonInteger query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return rightFind(query, startAsInt, endAsInt);
    }

    public PythonInteger rightIndex(PythonInteger query) {
        return rightIndex(query, 0, valueBuffer.limit());
    }

    public PythonInteger rightIndex(PythonInteger query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return rightIndex(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger rightIndex(PythonInteger query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return rightIndex(query, startAsInt, endAsInt);
    }

    private PythonInteger rightFind(PythonBytesLikeObject query, int start, int end) {
        byte[] queryBytes = query.asByteArray();

        for (int i = end - queryBytes.length; i >= start; i--) {
            int j = 0;
            for (; j < queryBytes.length; j++) {
                if (valueBuffer.get(i + j) != queryBytes[j]) {
                    break;
                }
            }

            if (j == queryBytes.length) {
                return PythonInteger.valueOf(i);
            }
        }

        return PythonInteger.valueOf(-1);
    }

    private PythonInteger rightIndex(PythonBytesLikeObject query, int start, int end) {
        byte[] queryBytes = query.asByteArray();

        for (int i = end - queryBytes.length; i >= start; i--) {
            int j = 0;
            for (; j < queryBytes.length; j++) {
                if (valueBuffer.get(i + j) != queryBytes[j]) {
                    break;
                }
            }

            if (j == queryBytes.length) {
                return PythonInteger.valueOf(i);
            }
        }

        throw new ValueError("Subsequence not found");
    }

    public PythonInteger rightFind(PythonByteArray query) {
        return rightFind(query, 0, valueBuffer.limit());
    }

    public PythonInteger rightFind(PythonByteArray query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return rightFind(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger rightFind(PythonByteArray query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return rightFind(query, startAsInt, endAsInt);
    }

    public PythonInteger rightIndex(PythonByteArray query) {
        return rightIndex(query, 0, valueBuffer.limit());
    }

    public PythonInteger rightIndex(PythonByteArray query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return rightIndex(query, startAsInt, valueBuffer.limit());
    }

    public PythonInteger rightIndex(PythonByteArray query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return rightIndex(query, startAsInt, endAsInt);
    }

    private PythonLikeTuple rightPartition(PythonBytesLikeObject sep, int start, int end) {
        byte[] sepBytes = sep.asByteArray();

        for (int i = end - sepBytes.length; i >= start; i--) {
            if (!Arrays.equals(valueBuffer.array(), i, i + sepBytes.length,
                    sepBytes, 0, sepBytes.length)) {
                continue;
            }

            return PythonLikeTuple.fromItems(
                    new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), 0, i)),
                    sep,
                    new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), i + sepBytes.length, valueBuffer.limit())));
        }

        return PythonLikeTuple.fromItems(
                new PythonByteArray(new byte[] {}),
                new PythonByteArray(new byte[] {}),
                this);
    }

    public PythonLikeTuple rightPartition(PythonByteArray sep) {
        return rightPartition(sep, 0, valueBuffer.limit());
    }

    public PythonBoolean startsWith(PythonByteArray prefix) {
        return PythonBoolean.valueOf(hasPrefix(prefix.valueBuffer, 0, valueBuffer.limit()));
    }

    public PythonBoolean startsWith(PythonByteArray prefix, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        return PythonBoolean.valueOf(hasPrefix(prefix.valueBuffer, startAsInt, valueBuffer.limit()));
    }

    public PythonBoolean startsWith(PythonByteArray prefix, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        return PythonBoolean.valueOf(hasPrefix(prefix.valueBuffer, startAsInt, endAsInt));
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonByteArray> prefixes) {
        for (PythonByteArray prefix : prefixes) {
            if (hasPrefix(prefix.valueBuffer, 0, valueBuffer.limit())) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonByteArray> prefixes, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());

        for (PythonByteArray prefix : prefixes) {
            if (hasPrefix(prefix.valueBuffer, startAsInt, valueBuffer.limit())) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonByteArray> prefixes, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, valueBuffer.limit());
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, valueBuffer.limit());

        for (PythonByteArray prefix : prefixes) {
            if (hasPrefix(prefix.valueBuffer, startAsInt, endAsInt)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonByteArray translate(PythonBytes table) {
        byte[] tableBytes = table.value;
        if (tableBytes.length != 256) {
            throw new ValueError("translate table must be a bytes object of length 256");
        }

        byte[] out = new byte[valueBuffer.limit()];

        for (int i = 0; i < valueBuffer.limit(); i++) {
            out[i] = tableBytes[valueBuffer.get(i) & 0xFF];
        }

        return new PythonByteArray(out);
    }

    public PythonByteArray translate(PythonByteArray table) {
        if (table.valueBuffer.limit() != 256) {
            throw new ValueError("translate table must be a bytes object of length 256");
        }

        byte[] out = new byte[valueBuffer.limit()];

        for (int i = 0; i < valueBuffer.limit(); i++) {
            out[i] = table.valueBuffer.get(valueBuffer.get(i) & 0xFF);
        }

        return new PythonByteArray(out);
    }

    public PythonByteArray translate(PythonNone table) {
        return this;
    }

    public PythonByteArray translate(PythonBytes table, PythonByteArray delete) {
        byte[] tableBytes = table.value;
        if (tableBytes.length != 256) {
            throw new ValueError("translate table must be a bytes object of length 256");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(valueBuffer.limit());
        BitSet removedSet = asBitSet(delete);

        for (int i = 0; i < valueBuffer.limit(); i++) {
            byte b = valueBuffer.get(i);
            if (!removedSet.get(b & 0xFF)) {
                out.write(tableBytes, b & 0xFF, 1);
            }
        }

        return new PythonByteArray(out.toByteArray());
    }

    public PythonByteArray translate(PythonNone table, PythonByteArray delete) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(valueBuffer.limit());
        BitSet removedSet = asBitSet(delete);

        for (int i = 0; i < valueBuffer.limit(); i++) {
            if (!removedSet.get(valueBuffer.get(i) & 0xFF)) {
                out.write(valueBuffer.array(), i, 1);
            }
        }

        return new PythonByteArray(out.toByteArray());
    }

    public PythonByteArray translate(PythonByteArray table, PythonByteArray delete) {
        if (table.valueBuffer.limit() != 256) {
            throw new ValueError("translate table must be a bytes object of length 256");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(valueBuffer.limit());
        BitSet removedSet = asBitSet(delete);

        for (int i = 0; i < valueBuffer.limit(); i++) {
            byte b = valueBuffer.get(i);
            if (!removedSet.get(b & 0xFF)) {
                out.write(table.valueBuffer.array(), b & 0xFF, 1);
            }
        }

        return new PythonByteArray(out.toByteArray());
    }

    public PythonByteArray center(PythonInteger fillWidth) {
        return center(fillWidth, ASCII_SPACE);
    }

    public PythonByteArray center(PythonInteger fillWidth, PythonByteArray fillCharacter) {
        if (fillCharacter.valueBuffer.limit() != 1) {
            throw new TypeError("center() argument 2 must be a byte string of length 1");
        }

        int widthAsInt = fillWidth.value.intValueExact();
        if (widthAsInt <= valueBuffer.limit()) {
            return this;
        }
        int extraWidth = widthAsInt - valueBuffer.limit();
        int rightPadding = extraWidth / 2;
        // left padding get extra character if extraWidth is odd
        int leftPadding = rightPadding + (extraWidth & 1); // x & 1 == x % 2

        byte[] out = new byte[widthAsInt];
        Arrays.fill(out, 0, leftPadding, fillCharacter.valueBuffer.get(0));
        System.arraycopy(valueBuffer.array(), 0, out, leftPadding, valueBuffer.limit());
        Arrays.fill(out, leftPadding + valueBuffer.limit(), widthAsInt, fillCharacter.valueBuffer.get(0));

        return new PythonByteArray(out);
    }

    public PythonByteArray leftJustify(PythonInteger fillWidth) {
        return leftJustify(fillWidth, ASCII_SPACE);
    }

    public PythonByteArray leftJustify(PythonInteger fillWidth, PythonByteArray fillCharacter) {
        if (fillCharacter.valueBuffer.limit() != 1) {
            throw new TypeError("ljust() argument 2 must be a byte string of length 1");
        }

        int widthAsInt = fillWidth.value.intValueExact();
        if (widthAsInt <= valueBuffer.limit()) {
            return this;
        }

        byte[] out = new byte[widthAsInt];
        System.arraycopy(valueBuffer.array(), 0, out, 0, valueBuffer.limit());
        Arrays.fill(out, valueBuffer.limit(), widthAsInt, fillCharacter.valueBuffer.get(0));

        return new PythonByteArray(out);
    }

    public PythonByteArray rightJustify(PythonInteger fillWidth) {
        return rightJustify(fillWidth, ASCII_SPACE);
    }

    public PythonByteArray rightJustify(PythonInteger fillWidth, PythonByteArray fillCharacter) {
        if (fillCharacter.valueBuffer.limit() != 1) {
            throw new TypeError("rjust() argument 2 must be a byte string of length 1");
        }

        int widthAsInt = fillWidth.value.intValueExact();
        if (widthAsInt <= valueBuffer.limit()) {
            return this;
        }

        int extraWidth = widthAsInt - valueBuffer.limit();

        byte[] out = new byte[widthAsInt];
        Arrays.fill(out, 0, extraWidth, fillCharacter.valueBuffer.get(0));
        System.arraycopy(valueBuffer.array(), 0, out, extraWidth, valueBuffer.limit());

        return new PythonByteArray(out);
    }

    public PythonByteArray strip() {
        return strip(ASCII_SPACE);
    }

    public PythonByteArray strip(PythonNone ignored) {
        return strip();
    }

    public PythonByteArray strip(PythonByteArray bytesToStrip) {
        BitSet toStrip = asBitSet(bytesToStrip);

        int start = 0;
        int end = valueBuffer.limit() - 1;

        while (start < valueBuffer.limit() && toStrip.get(valueBuffer.get(start) & 0xFF)) {
            start++;
        }

        while (end >= start && toStrip.get(valueBuffer.get(end) & 0xFF)) {
            end--;
        }

        if (end < start) {
            return new PythonByteArray(new byte[] {});
        }

        return new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), start, end + 1));
    }

    public PythonByteArray leftStrip() {
        return leftStrip(ASCII_SPACE);
    }

    public PythonByteArray leftStrip(PythonNone ignored) {
        return leftStrip();
    }

    public PythonByteArray leftStrip(PythonByteArray bytesToStrip) {
        BitSet toStrip = asBitSet(bytesToStrip);

        int start = 0;

        while (start < valueBuffer.limit() && toStrip.get(valueBuffer.get(start) & 0xFF)) {
            start++;
        }

        if (start == valueBuffer.limit()) {
            return new PythonByteArray(new byte[] {});
        }

        return new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), start, valueBuffer.limit()));
    }

    public PythonByteArray rightStrip() {
        return rightStrip(ASCII_SPACE);
    }

    public PythonByteArray rightStrip(PythonNone ignored) {
        return rightStrip();
    }

    public PythonByteArray rightStrip(PythonByteArray bytesToStrip) {
        BitSet toStrip = asBitSet(bytesToStrip);

        int end = valueBuffer.limit() - 1;

        while (end >= 0 && toStrip.get(valueBuffer.get(end) & 0xFF)) {
            end--;
        }

        if (end < 0) {
            return new PythonByteArray(new byte[] {});
        }

        return new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), 0, end + 1));
    }

    public PythonLikeList<PythonByteArray> split() {
        PythonLikeList<PythonByteArray> out = new PythonLikeList<>();
        int start = 0;
        int end = valueBuffer.limit();

        while (end > 0 && ASCII_WHITESPACE_BITSET.get(valueBuffer.get(end - 1) & 0xFF)) {
            end--;
        }

        while (start < end && ASCII_WHITESPACE_BITSET.get(valueBuffer.get(start) & 0xFF)) {
            start++;
        }

        if (start == end) {
            return out;
        }

        int lastEnd = start;
        while (start < end - 1) {
            while (start < end - 1 &&
                    !ASCII_WHITESPACE_BITSET.get(valueBuffer.get(start) & 0xFF)) {
                start++;
            }
            if (start != end - 1) {
                out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, start)));
                lastEnd = start + 1;
                start = lastEnd;
            }
        }

        if (lastEnd != end) {
            out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, end)));
        }

        return out;
    }

    public PythonLikeList<PythonByteArray> split(PythonNone ignored) {
        return split();
    }

    public PythonLikeList<PythonByteArray> split(PythonByteArray seperator) {
        PythonLikeList<PythonByteArray> out = new PythonLikeList<>();
        int start = 0;
        int end = valueBuffer.limit();

        int lastEnd = start;
        while (start < end - seperator.valueBuffer.limit()) {
            while (start < end - seperator.valueBuffer.limit() &&
                    !Arrays.equals(valueBuffer.array(), start, start + seperator.valueBuffer.limit(),
                            seperator.valueBuffer.array(), 0, seperator.valueBuffer.limit())) {
                start++;
            }
            if (start != end - seperator.valueBuffer.limit()) {
                out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, start)));
                lastEnd = start + seperator.valueBuffer.limit();
                start = lastEnd;
            }
        }

        if (Arrays.equals(valueBuffer.array(), start, start + seperator.valueBuffer.limit(),
                seperator.valueBuffer.array(), 0, seperator.valueBuffer.limit())) {
            out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, start)));
            lastEnd = start + seperator.valueBuffer.limit();
        }

        out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, end)));
        return out;
    }

    public PythonLikeList<PythonByteArray> split(PythonByteArray seperator, PythonInteger maxSplits) {
        if (maxSplits.equals(new PythonInteger(-1))) {
            return split(seperator);
        }

        PythonLikeList<PythonByteArray> out = new PythonLikeList<>();
        int start = 0;
        int end = valueBuffer.limit();

        int lastEnd = start;
        while (start < end - seperator.valueBuffer.limit() && maxSplits.compareTo(PythonInteger.ONE) >= 0) {
            while (start < end - seperator.valueBuffer.limit() &&
                    !Arrays.equals(valueBuffer.array(), start, start + seperator.valueBuffer.limit(),
                            seperator.valueBuffer.array(), 0, seperator.valueBuffer.limit())) {
                start++;
            }
            if (start != end - seperator.valueBuffer.limit()) {
                out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, start)));
                lastEnd = start + seperator.valueBuffer.limit();
                start = lastEnd;
                maxSplits = maxSplits.subtract(PythonInteger.ONE);
            }
        }

        if (maxSplits.compareTo(PythonInteger.ONE) >= 0 &&
                Arrays.equals(valueBuffer.array(), start, start + seperator.valueBuffer.limit(),
                        seperator.valueBuffer.array(), 0, seperator.valueBuffer.limit())) {
            out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, start)));
            lastEnd = start + seperator.valueBuffer.limit();
        }

        out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, end)));
        return out;
    }

    public PythonLikeList<PythonByteArray> split(PythonNone seperator, PythonInteger maxSplits) {
        if (maxSplits.equals(new PythonInteger(-1))) {
            return split(seperator);
        }

        PythonLikeList<PythonByteArray> out = new PythonLikeList<>();
        int start = 0;
        int end = valueBuffer.limit();

        while (end > 0 && ASCII_WHITESPACE_BITSET.get(valueBuffer.get(end - 1) & 0xFF)) {
            end--;
        }

        while (start < end && ASCII_WHITESPACE_BITSET.get(valueBuffer.get(start) & 0xFF)) {
            start++;
        }

        if (start == end) {
            return out;
        }

        int lastEnd = start;
        while (start < end - 1 && maxSplits.compareTo(PythonInteger.ONE) >= 0) {
            while (start < end - 1 && !ASCII_WHITESPACE_BITSET.get(valueBuffer.get(start) & 0xFF)) {
                start++;
            }
            if (start != end - 1) {
                out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, start)));
                lastEnd = start + 1;
                start = lastEnd;
                maxSplits = maxSplits.subtract(PythonInteger.ONE);
            }
        }

        if (lastEnd != end) {
            out.add(new PythonByteArray(Arrays.copyOfRange(valueBuffer.array(), lastEnd, end)));
        }

        return out;
    }

    public PythonLikeList<PythonByteArray> rightSplit() {
        return split();
    }

    public PythonLikeList<PythonByteArray> rightSplit(PythonNone ignored) {
        return rightSplit();
    }

    public PythonLikeList<PythonByteArray> rightSplit(PythonByteArray seperator) {
        return split(seperator);
    }

    private static byte[] reverseInplace(byte[] array) {
        for (int i = 0; i < array.length >> 1; i++) {
            byte temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
        return array;
    }

    public PythonLikeList<PythonByteArray> rightSplit(PythonByteArray seperator, PythonInteger maxSplits) {
        if (maxSplits.equals(new PythonInteger(-1))) {
            return split(seperator);
        }

        PythonLikeList<PythonByteArray> out = new PythonLikeList<>();

        byte[] reversedValue = reverseInplace(valueBuffer.array().clone());
        byte[] reversedSep = reverseInplace(seperator.valueBuffer.array().clone());

        int start = 0;
        int end = reversedValue.length;

        int lastEnd = start;
        while (start < end - reversedSep.length && maxSplits.compareTo(PythonInteger.ONE) >= 0) {
            while (start < end - reversedSep.length &&
                    !Arrays.equals(reversedValue, start, start + reversedSep.length,
                            reversedSep, 0, reversedSep.length)) {
                start++;
            }
            if (start != end - reversedSep.length) {
                out.add(new PythonByteArray(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, start))));
                lastEnd = start + reversedSep.length;
                start = lastEnd;
                maxSplits = maxSplits.subtract(PythonInteger.ONE);
            }
        }

        if (maxSplits.compareTo(PythonInteger.ONE) >= 0 &&
                Arrays.equals(reversedValue, start, start + reversedSep.length,
                        reversedSep, 0, reversedSep.length)) {
            out.add(new PythonByteArray(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, start))));
            lastEnd = start + seperator.valueBuffer.limit();
        }

        out.add(new PythonByteArray(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, end))));
        out.reverse();
        return out;
    }

    public PythonLikeList<PythonByteArray> rightSplit(PythonNone seperator, PythonInteger maxSplits) {
        if (maxSplits.equals(new PythonInteger(-1))) {
            return split(seperator);
        }

        PythonLikeList<PythonByteArray> out = new PythonLikeList<>();

        byte[] reversedValue = reverseInplace(Arrays.copyOfRange(valueBuffer.array(), 0, valueBuffer.limit()));

        int start = 0;
        int end = valueBuffer.limit();

        while (end > 0 && ASCII_WHITESPACE_BITSET.get(valueBuffer.get(end - 1) & 0xFF)) {
            end--;
        }

        while (start < end && ASCII_WHITESPACE_BITSET.get(valueBuffer.get(start) & 0xFF)) {
            start++;
        }

        if (start == end) {
            return out;
        }

        int lastEnd = start;
        while (start < end - 1 && maxSplits.compareTo(PythonInteger.ONE) >= 0) {
            while (start < end - 1 &&
                    !ASCII_WHITESPACE_BITSET.get(reversedValue[start] & 0xFF)) {
                start++;
            }
            if (start != end - 1) {
                out.add(new PythonByteArray(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, start))));
                lastEnd = start + 1;
                start = lastEnd;
                maxSplits = maxSplits.subtract(PythonInteger.ONE);
            }
        }

        if (lastEnd != end) {
            out.add(new PythonByteArray(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, end))));
        }

        out.reverse();
        return out;
    }

    public PythonByteArray capitalize() {
        var asString = asAsciiString();
        if (asString.value.isEmpty()) {
            return asString.asAsciiByteArray();
        }
        var tail = PythonString.valueOf(asString.value.substring(1))
                .withModifiedCodepoints(cp -> cp < 128 ? Character.toLowerCase(cp) : cp).value;
        var head = asString.value.charAt(0);
        if (head < 128) {
            head = Character.toTitleCase(head);
        }
        return (PythonString.valueOf(head + tail)).asAsciiByteArray();
    }

    public PythonByteArray expandTabs() {
        return asAsciiString().expandTabs().asAsciiByteArray();
    }

    public PythonByteArray expandTabs(PythonInteger tabSize) {
        return asAsciiString().expandTabs(tabSize).asAsciiByteArray();
    }

    public PythonBoolean isAlphaNumeric() {
        return asAsciiString().isAlphaNumeric();
    }

    public PythonBoolean isAlpha() {
        return asAsciiString().isAlpha();
    }

    public PythonBoolean isAscii() {
        for (int i = 0; i < valueBuffer.limit(); i++) {
            byte b = valueBuffer.get(i);
            if ((b & 0xFF) > 0x7F) {
                return PythonBoolean.FALSE;
            }
        }
        return PythonBoolean.TRUE;
    }

    public PythonBoolean isDigit() {
        return asAsciiString().isDigit();
    }

    public PythonBoolean isLower() {
        return asAsciiString().isLower();
    }

    public PythonBoolean isSpace() {
        return asAsciiString().isSpace();
    }

    public PythonBoolean isTitle() {
        return asAsciiString().isTitle();
    }

    public PythonBoolean isUpper() {
        return asAsciiString().isUpper();
    }

    public PythonByteArray lower() {
        return asAsciiString().withModifiedCodepoints(
                cp -> cp < 128 ? Character.toLowerCase(cp) : cp).asAsciiByteArray();
    }

    public PythonLikeList<PythonByteArray> splitLines() {
        return asAsciiString().splitLines()
                .stream()
                .map(PythonString::asAsciiByteArray)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonByteArray> splitLines(PythonBoolean keepEnds) {
        return asAsciiString().splitLines(keepEnds)
                .stream()
                .map(PythonString::asAsciiByteArray)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonByteArray swapCase() {
        return asAsciiString().withModifiedCodepoints(
                cp -> cp < 128 ? PythonString.CharacterCase.swapCase(cp) : cp).asAsciiByteArray();
    }

    public PythonByteArray title() {
        return asAsciiString().title(cp -> cp < 128).asAsciiByteArray();
    }

    public PythonByteArray upper() {
        return asAsciiString().withModifiedCodepoints(
                cp -> cp < 128 ? Character.toUpperCase(cp) : cp).asAsciiByteArray();
    }

    public PythonByteArray zfill(PythonInteger width) {
        return asAsciiString().zfill(width).asAsciiByteArray();
    }

    public PythonString asString() {
        return PythonString.valueOf(toString());
    }

    public PythonString repr() {
        return asString();
    }

    @Override
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(valueBuffer.limit());
        out.append("bytearray(");

        out.append(new PythonBytes(Arrays.copyOfRange(valueBuffer.array(), 0, valueBuffer.limit())).repr().value);

        out.append(")");

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
        PythonByteArray that = (PythonByteArray) o;
        return valueBuffer.equals(that.valueBuffer);
    }

    @Override
    public int hashCode() {
        return valueBuffer.hashCode();
    }
}
