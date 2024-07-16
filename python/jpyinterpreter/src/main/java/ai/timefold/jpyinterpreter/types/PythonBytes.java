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
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

public class PythonBytes extends AbstractPythonLikeObject implements PythonBytesLikeObject, PlanningImmutable {

    public static final PythonBytes EMPTY = new PythonBytes(new byte[0]);

    private static final PythonBytes ASCII_SPACE = new PythonBytes(new byte[] { ' ' });
    private static final BitSet ASCII_WHITESPACE_BITSET = asBitSet(new PythonBytes(
            new byte[] { ' ', '\t', '\n', '\r', 0x0b, '\f' }));

    public static final PythonInteger[] BYTE_TO_INT = new PythonInteger[] {
            PythonInteger.valueOf(0x00), PythonInteger.valueOf(0x01), PythonInteger.valueOf(0x02), PythonInteger.valueOf(0x03),
            PythonInteger.valueOf(0x04), PythonInteger.valueOf(0x05), PythonInteger.valueOf(0x06), PythonInteger.valueOf(0x07),
            PythonInteger.valueOf(0x08), PythonInteger.valueOf(0x09), PythonInteger.valueOf(0x0a), PythonInteger.valueOf(0x0b),
            PythonInteger.valueOf(0x0c), PythonInteger.valueOf(0x0d), PythonInteger.valueOf(0x0e), PythonInteger.valueOf(0x0f),

            PythonInteger.valueOf(0x10), PythonInteger.valueOf(0x11), PythonInteger.valueOf(0x12), PythonInteger.valueOf(0x13),
            PythonInteger.valueOf(0x14), PythonInteger.valueOf(0x15), PythonInteger.valueOf(0x16), PythonInteger.valueOf(0x17),
            PythonInteger.valueOf(0x18), PythonInteger.valueOf(0x19), PythonInteger.valueOf(0x1a), PythonInteger.valueOf(0x1b),
            PythonInteger.valueOf(0x1c), PythonInteger.valueOf(0x1d), PythonInteger.valueOf(0x1e), PythonInteger.valueOf(0x1f),

            PythonInteger.valueOf(0x20), PythonInteger.valueOf(0x21), PythonInteger.valueOf(0x22), PythonInteger.valueOf(0x23),
            PythonInteger.valueOf(0x24), PythonInteger.valueOf(0x25), PythonInteger.valueOf(0x26), PythonInteger.valueOf(0x27),
            PythonInteger.valueOf(0x28), PythonInteger.valueOf(0x29), PythonInteger.valueOf(0x2a), PythonInteger.valueOf(0x2b),
            PythonInteger.valueOf(0x2c), PythonInteger.valueOf(0x2d), PythonInteger.valueOf(0x2e), PythonInteger.valueOf(0x2f),

            PythonInteger.valueOf(0x30), PythonInteger.valueOf(0x31), PythonInteger.valueOf(0x32), PythonInteger.valueOf(0x33),
            PythonInteger.valueOf(0x34), PythonInteger.valueOf(0x35), PythonInteger.valueOf(0x36), PythonInteger.valueOf(0x37),
            PythonInteger.valueOf(0x38), PythonInteger.valueOf(0x39), PythonInteger.valueOf(0x3a), PythonInteger.valueOf(0x3b),
            PythonInteger.valueOf(0x3c), PythonInteger.valueOf(0x3d), PythonInteger.valueOf(0x3e), PythonInteger.valueOf(0x3f),

            PythonInteger.valueOf(0x40), PythonInteger.valueOf(0x41), PythonInteger.valueOf(0x42), PythonInteger.valueOf(0x43),
            PythonInteger.valueOf(0x44), PythonInteger.valueOf(0x45), PythonInteger.valueOf(0x46), PythonInteger.valueOf(0x47),
            PythonInteger.valueOf(0x48), PythonInteger.valueOf(0x49), PythonInteger.valueOf(0x4a), PythonInteger.valueOf(0x4b),
            PythonInteger.valueOf(0x4c), PythonInteger.valueOf(0x4d), PythonInteger.valueOf(0x4e), PythonInteger.valueOf(0x4f),

            PythonInteger.valueOf(0x50), PythonInteger.valueOf(0x51), PythonInteger.valueOf(0x52), PythonInteger.valueOf(0x53),
            PythonInteger.valueOf(0x54), PythonInteger.valueOf(0x55), PythonInteger.valueOf(0x56), PythonInteger.valueOf(0x57),
            PythonInteger.valueOf(0x58), PythonInteger.valueOf(0x59), PythonInteger.valueOf(0x5a), PythonInteger.valueOf(0x5b),
            PythonInteger.valueOf(0x5c), PythonInteger.valueOf(0x5d), PythonInteger.valueOf(0x5e), PythonInteger.valueOf(0x5f),

            PythonInteger.valueOf(0x60), PythonInteger.valueOf(0x61), PythonInteger.valueOf(0x62), PythonInteger.valueOf(0x63),
            PythonInteger.valueOf(0x64), PythonInteger.valueOf(0x65), PythonInteger.valueOf(0x66), PythonInteger.valueOf(0x67),
            PythonInteger.valueOf(0x68), PythonInteger.valueOf(0x69), PythonInteger.valueOf(0x6a), PythonInteger.valueOf(0x6b),
            PythonInteger.valueOf(0x6c), PythonInteger.valueOf(0x6d), PythonInteger.valueOf(0x6e), PythonInteger.valueOf(0x6f),

            PythonInteger.valueOf(0x70), PythonInteger.valueOf(0x71), PythonInteger.valueOf(0x72), PythonInteger.valueOf(0x73),
            PythonInteger.valueOf(0x74), PythonInteger.valueOf(0x75), PythonInteger.valueOf(0x76), PythonInteger.valueOf(0x77),
            PythonInteger.valueOf(0x78), PythonInteger.valueOf(0x79), PythonInteger.valueOf(0x7a), PythonInteger.valueOf(0x7b),
            PythonInteger.valueOf(0x7c), PythonInteger.valueOf(0x7d), PythonInteger.valueOf(0x7e), PythonInteger.valueOf(0x7f),

            PythonInteger.valueOf(0x80), PythonInteger.valueOf(0x81), PythonInteger.valueOf(0x82), PythonInteger.valueOf(0x83),
            PythonInteger.valueOf(0x84), PythonInteger.valueOf(0x85), PythonInteger.valueOf(0x86), PythonInteger.valueOf(0x87),
            PythonInteger.valueOf(0x88), PythonInteger.valueOf(0x89), PythonInteger.valueOf(0x8a), PythonInteger.valueOf(0x8b),
            PythonInteger.valueOf(0x8c), PythonInteger.valueOf(0x8d), PythonInteger.valueOf(0x8e), PythonInteger.valueOf(0x8f),

            PythonInteger.valueOf(0x90), PythonInteger.valueOf(0x91), PythonInteger.valueOf(0x92), PythonInteger.valueOf(0x93),
            PythonInteger.valueOf(0x94), PythonInteger.valueOf(0x95), PythonInteger.valueOf(0x96), PythonInteger.valueOf(0x97),
            PythonInteger.valueOf(0x98), PythonInteger.valueOf(0x99), PythonInteger.valueOf(0x9a), PythonInteger.valueOf(0x9b),
            PythonInteger.valueOf(0x9c), PythonInteger.valueOf(0x9d), PythonInteger.valueOf(0x9e), PythonInteger.valueOf(0x9f),

            PythonInteger.valueOf(0xa0), PythonInteger.valueOf(0xa1), PythonInteger.valueOf(0xa2), PythonInteger.valueOf(0xa3),
            PythonInteger.valueOf(0xa4), PythonInteger.valueOf(0xa5), PythonInteger.valueOf(0xa6), PythonInteger.valueOf(0xa7),
            PythonInteger.valueOf(0xa8), PythonInteger.valueOf(0xa9), PythonInteger.valueOf(0xaa), PythonInteger.valueOf(0xab),
            PythonInteger.valueOf(0xac), PythonInteger.valueOf(0xad), PythonInteger.valueOf(0xae), PythonInteger.valueOf(0xaf),

            PythonInteger.valueOf(0xb0), PythonInteger.valueOf(0xb1), PythonInteger.valueOf(0xb2), PythonInteger.valueOf(0xb3),
            PythonInteger.valueOf(0xb4), PythonInteger.valueOf(0xb5), PythonInteger.valueOf(0xb6), PythonInteger.valueOf(0xb7),
            PythonInteger.valueOf(0xb8), PythonInteger.valueOf(0xb9), PythonInteger.valueOf(0xba), PythonInteger.valueOf(0xbb),
            PythonInteger.valueOf(0xbc), PythonInteger.valueOf(0xbd), PythonInteger.valueOf(0xbe), PythonInteger.valueOf(0xbf),

            PythonInteger.valueOf(0xc0), PythonInteger.valueOf(0xc1), PythonInteger.valueOf(0xc2), PythonInteger.valueOf(0xc3),
            PythonInteger.valueOf(0xc4), PythonInteger.valueOf(0xc5), PythonInteger.valueOf(0xc6), PythonInteger.valueOf(0xc7),
            PythonInteger.valueOf(0xc8), PythonInteger.valueOf(0xc9), PythonInteger.valueOf(0xca), PythonInteger.valueOf(0xcb),
            PythonInteger.valueOf(0xcc), PythonInteger.valueOf(0xcd), PythonInteger.valueOf(0xce), PythonInteger.valueOf(0xcf),

            PythonInteger.valueOf(0xd0), PythonInteger.valueOf(0xd1), PythonInteger.valueOf(0xd2), PythonInteger.valueOf(0xd3),
            PythonInteger.valueOf(0xd4), PythonInteger.valueOf(0xd5), PythonInteger.valueOf(0xd6), PythonInteger.valueOf(0xd7),
            PythonInteger.valueOf(0xd8), PythonInteger.valueOf(0xd9), PythonInteger.valueOf(0xda), PythonInteger.valueOf(0xdb),
            PythonInteger.valueOf(0xdc), PythonInteger.valueOf(0xdd), PythonInteger.valueOf(0xde), PythonInteger.valueOf(0xdf),

            PythonInteger.valueOf(0xe0), PythonInteger.valueOf(0xe1), PythonInteger.valueOf(0xe2), PythonInteger.valueOf(0xe3),
            PythonInteger.valueOf(0xe4), PythonInteger.valueOf(0xe5), PythonInteger.valueOf(0xe6), PythonInteger.valueOf(0xe7),
            PythonInteger.valueOf(0xe8), PythonInteger.valueOf(0xe9), PythonInteger.valueOf(0xea), PythonInteger.valueOf(0xeb),
            PythonInteger.valueOf(0xec), PythonInteger.valueOf(0xed), PythonInteger.valueOf(0xee), PythonInteger.valueOf(0xef),

            PythonInteger.valueOf(0xf0), PythonInteger.valueOf(0xf1), PythonInteger.valueOf(0xf2), PythonInteger.valueOf(0xf3),
            PythonInteger.valueOf(0xf4), PythonInteger.valueOf(0xf5), PythonInteger.valueOf(0xf6), PythonInteger.valueOf(0xf7),
            PythonInteger.valueOf(0xf8), PythonInteger.valueOf(0xf9), PythonInteger.valueOf(0xfa), PythonInteger.valueOf(0xfb),
            PythonInteger.valueOf(0xfc), PythonInteger.valueOf(0xfd), PythonInteger.valueOf(0xfe), PythonInteger.valueOf(0xff),
    };

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonBytes::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        BuiltinTypes.BYTES_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
            if (positionalArguments.isEmpty()) {
                return new PythonBytes(new byte[] {});
            } else if (positionalArguments.size() == 1) {
                PythonLikeObject arg = positionalArguments.get(0);
                if (arg instanceof PythonInteger) {
                    return new PythonBytes(new byte[((PythonInteger) arg).value.intValueExact()]);
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
                    return new PythonBytes(out.toByteArray());
                }
            } else {
                throw new ValueError("bytearray takes 0 or 1 arguments, not " + positionalArguments.size());
            }
        }));

        // Unary
        BuiltinTypes.BYTES_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION, PythonBytes.class.getMethod("repr"));
        BuiltinTypes.BYTES_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING, PythonBytes.class.getMethod("asString"));
        BuiltinTypes.BYTES_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR, PythonBytes.class.getMethod("getIterator"));
        BuiltinTypes.BYTES_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, PythonBytes.class.getMethod("getLength"));

        // Binary
        BuiltinTypes.BYTES_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonBytes.class.getMethod("getCharAt", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonBytes.class.getMethod("getSubsequence", PythonSlice.class));
        BuiltinTypes.BYTES_TYPE.addBinaryMethod(PythonBinaryOperator.CONTAINS,
                PythonBytes.class.getMethod("containsSubsequence", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addBinaryMethod(PythonBinaryOperator.ADD,
                PythonBytes.class.getMethod("concat", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addBinaryMethod(PythonBinaryOperator.MULTIPLY,
                PythonBytes.class.getMethod("repeat", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonBytes.class.getMethod("interpolate", PythonLikeObject.class));
        BuiltinTypes.BYTES_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonBytes.class.getMethod("interpolate", PythonLikeTuple.class));
        BuiltinTypes.BYTES_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonBytes.class.getMethod("interpolate", PythonLikeDict.class));

        // Other
        BuiltinTypes.BYTES_TYPE.addMethod("capitalize", PythonBytes.class.getMethod("capitalize"));

        BuiltinTypes.BYTES_TYPE.addMethod("center", PythonBytes.class.getMethod("center", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("center",
                PythonBytes.class.getMethod("center", PythonInteger.class, PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("count", PythonBytes.class.getMethod("count", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("count",
                PythonBytes.class.getMethod("count", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("count",
                PythonBytes.class.getMethod("count", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("count", PythonBytes.class.getMethod("count", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("count",
                PythonBytes.class.getMethod("count", PythonBytes.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("count",
                PythonBytes.class.getMethod("count", PythonBytes.class, PythonInteger.class, PythonInteger.class));

        // TODO: decode

        BuiltinTypes.BYTES_TYPE.addMethod("endswith", PythonBytes.class.getMethod("endsWith", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("endswith", PythonBytes.class.getMethod("endsWith", PythonLikeTuple.class));
        BuiltinTypes.BYTES_TYPE.addMethod("endswith",
                PythonBytes.class.getMethod("endsWith", PythonBytes.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("endswith",
                PythonBytes.class.getMethod("endsWith", PythonLikeTuple.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("endswith",
                PythonBytes.class.getMethod("endsWith", PythonBytes.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("endswith",
                PythonBytes.class.getMethod("endsWith", PythonLikeTuple.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("expandtabs", PythonBytes.class.getMethod("expandTabs"));
        BuiltinTypes.BYTES_TYPE.addMethod("expandtabs", PythonBytes.class.getMethod("expandTabs", PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("find", PythonBytes.class.getMethod("find", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("find", PythonBytes.class.getMethod("find", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("find", PythonBytes.class.getMethod("find", PythonBytes.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("find",
                PythonBytes.class.getMethod("find", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("find",
                PythonBytes.class.getMethod("find", PythonBytes.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("find",
                PythonBytes.class.getMethod("find", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("index", PythonBytes.class.getMethod("index", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("index", PythonBytes.class.getMethod("index", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("index",
                PythonBytes.class.getMethod("index", PythonBytes.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("index",
                PythonBytes.class.getMethod("index", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("index",
                PythonBytes.class.getMethod("index", PythonBytes.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("index",
                PythonBytes.class.getMethod("index", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("isalnum", PythonBytes.class.getMethod("isAlphaNumeric"));
        BuiltinTypes.BYTES_TYPE.addMethod("isalpha", PythonBytes.class.getMethod("isAlpha"));
        BuiltinTypes.BYTES_TYPE.addMethod("isascii", PythonBytes.class.getMethod("isAscii"));
        BuiltinTypes.BYTES_TYPE.addMethod("isdigit", PythonBytes.class.getMethod("isDigit"));
        BuiltinTypes.BYTES_TYPE.addMethod("islower", PythonBytes.class.getMethod("isLower"));
        BuiltinTypes.BYTES_TYPE.addMethod("isspace", PythonBytes.class.getMethod("isSpace"));
        BuiltinTypes.BYTES_TYPE.addMethod("istitle", PythonBytes.class.getMethod("isTitle"));
        BuiltinTypes.BYTES_TYPE.addMethod("isupper", PythonBytes.class.getMethod("isUpper"));

        BuiltinTypes.BYTES_TYPE.addMethod("join", PythonBytes.class.getMethod("join", PythonLikeObject.class));

        BuiltinTypes.BYTES_TYPE.addMethod("ljust", PythonBytes.class.getMethod("leftJustify", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("ljust",
                PythonBytes.class.getMethod("leftJustify", PythonInteger.class, PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("lower", PythonBytes.class.getMethod("lower"));

        BuiltinTypes.BYTES_TYPE.addMethod("lstrip", PythonBytes.class.getMethod("leftStrip"));
        BuiltinTypes.BYTES_TYPE.addMethod("lstrip", PythonBytes.class.getMethod("leftStrip", PythonNone.class));
        BuiltinTypes.BYTES_TYPE.addMethod("lstrip", PythonBytes.class.getMethod("leftStrip", PythonBytes.class));

        // TODO: maketrans

        BuiltinTypes.BYTES_TYPE.addMethod("partition", PythonBytes.class.getMethod("partition", PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("removeprefix", PythonBytes.class.getMethod("removePrefix", PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("removesuffix", PythonBytes.class.getMethod("removeSuffix", PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("replace",
                PythonBytes.class.getMethod("replace", PythonBytes.class, PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("replace",
                PythonBytes.class.getMethod("replace", PythonBytes.class, PythonBytes.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("rfind", PythonBytes.class.getMethod("rightFind", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rfind", PythonBytes.class.getMethod("rightFind", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rfind",
                PythonBytes.class.getMethod("rightFind", PythonBytes.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rfind",
                PythonBytes.class.getMethod("rightFind", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rfind",
                PythonBytes.class.getMethod("rightFind", PythonBytes.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rfind",
                PythonBytes.class.getMethod("rightFind", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("rindex", PythonBytes.class.getMethod("rightIndex", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rindex", PythonBytes.class.getMethod("rightIndex", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rindex",
                PythonBytes.class.getMethod("rightIndex", PythonBytes.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rindex",
                PythonBytes.class.getMethod("rightIndex", PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rindex",
                PythonBytes.class.getMethod("rightIndex", PythonBytes.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rindex",
                PythonBytes.class.getMethod("rightIndex", PythonInteger.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("rjust", PythonBytes.class.getMethod("rightJustify", PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rjust",
                PythonBytes.class.getMethod("rightJustify", PythonInteger.class, PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("rpartition", PythonBytes.class.getMethod("rightPartition", PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("rsplit", PythonBytes.class.getMethod("rightSplit"));
        BuiltinTypes.BYTES_TYPE.addMethod("rsplit", PythonBytes.class.getMethod("rightSplit", PythonNone.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rsplit", PythonBytes.class.getMethod("rightSplit", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rsplit",
                PythonBytes.class.getMethod("rightSplit", PythonNone.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rsplit",
                PythonBytes.class.getMethod("rightSplit", PythonBytes.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("rstrip", PythonBytes.class.getMethod("rightStrip"));
        BuiltinTypes.BYTES_TYPE.addMethod("rstrip", PythonBytes.class.getMethod("rightStrip", PythonNone.class));
        BuiltinTypes.BYTES_TYPE.addMethod("rstrip", PythonBytes.class.getMethod("rightStrip", PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("split", PythonBytes.class.getMethod("split"));
        BuiltinTypes.BYTES_TYPE.addMethod("split", PythonBytes.class.getMethod("split", PythonNone.class));
        BuiltinTypes.BYTES_TYPE.addMethod("split", PythonBytes.class.getMethod("split", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("split", PythonBytes.class.getMethod("split", PythonNone.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("split",
                PythonBytes.class.getMethod("split", PythonBytes.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("splitlines", PythonBytes.class.getMethod("splitLines"));
        BuiltinTypes.BYTES_TYPE.addMethod("splitlines", PythonBytes.class.getMethod("splitLines", PythonBoolean.class));

        BuiltinTypes.BYTES_TYPE.addMethod("startswith", PythonBytes.class.getMethod("startsWith", PythonBytes.class));
        BuiltinTypes.BYTES_TYPE.addMethod("startswith", PythonBytes.class.getMethod("startsWith", PythonLikeTuple.class));
        BuiltinTypes.BYTES_TYPE.addMethod("startswith",
                PythonBytes.class.getMethod("startsWith", PythonBytes.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("startswith",
                PythonBytes.class.getMethod("startsWith", PythonLikeTuple.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("startswith",
                PythonBytes.class.getMethod("startsWith", PythonBytes.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.BYTES_TYPE.addMethod("startswith",
                PythonBytes.class.getMethod("startsWith", PythonLikeTuple.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.BYTES_TYPE.addMethod("strip", PythonBytes.class.getMethod("strip"));
        BuiltinTypes.BYTES_TYPE.addMethod("strip", PythonBytes.class.getMethod("strip", PythonNone.class));
        BuiltinTypes.BYTES_TYPE.addMethod("strip", PythonBytes.class.getMethod("strip", PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("swapcase", PythonBytes.class.getMethod("swapCase"));

        BuiltinTypes.BYTES_TYPE.addMethod("title", PythonBytes.class.getMethod("title"));

        BuiltinTypes.BYTES_TYPE.addMethod("translate", PythonBytes.class.getMethod("translate", PythonBytes.class));

        BuiltinTypes.BYTES_TYPE.addMethod("upper", PythonBytes.class.getMethod("upper"));

        BuiltinTypes.BYTES_TYPE.addMethod("zfill", PythonBytes.class.getMethod("zfill", PythonInteger.class));

        return BuiltinTypes.BYTES_TYPE;
    }

    public final byte[] value;

    public PythonBytes(byte[] value) {
        super(BuiltinTypes.BYTES_TYPE);
        this.value = value;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(value).asReadOnlyBuffer();
    }

    public final ByteCharSequence asCharSequence() {
        return new ByteCharSequence(value);
    }

    public final PythonString asAsciiString() {
        return PythonString.valueOf(asCharSequence().toString());
    }

    public static PythonBytes fromIntTuple(PythonLikeTuple tuple) {
        byte[] out = new byte[tuple.size()];
        IntStream.range(0, tuple.size()).forEach(index -> out[index] = ((PythonInteger) tuple.get(index)).asByte());
        return new PythonBytes(out);
    }

    public final PythonLikeTuple asIntTuple() {
        return IntStream.range(0, value.length).mapToObj(index -> BYTE_TO_INT[Byte.toUnsignedInt(value[index])])
                .collect(Collectors.toCollection(PythonLikeTuple::new));
    }

    private static BitSet asBitSet(PythonBytes bytesInBitSet) {
        BitSet out = new BitSet();
        for (byte item : bytesInBitSet.value) {
            out.set(item & 0xFF);
        }
        return out;
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(value.length);
    }

    public PythonInteger getCharAt(PythonInteger position) {
        int index = PythonSlice.asIntIndexForLength(position, value.length);

        if (index >= value.length) {
            throw new IndexError("position " + position + " larger than bytes length " + value.length);
        } else if (index < 0) {
            throw new IndexError("position " + position + " is less than 0");
        }

        return BYTE_TO_INT[Byte.toUnsignedInt(value[index])];
    }

    public PythonBytes getSubsequence(PythonSlice slice) {
        int length = value.length;
        int start = slice.getStartIndex(length);
        int stop = slice.getStopIndex(length);
        int step = slice.getStrideLength();

        if (step == 1) {
            if (stop <= start) {
                return EMPTY;
            } else {
                return new PythonBytes(Arrays.copyOfRange(value, start, stop));
            }
        } else {
            byte[] out = new byte[slice.getSliceSize(length)];
            slice.iterate(length, (index, iteration) -> {
                out[iteration] = value[index];
            });
            return new PythonBytes(out);
        }
    }

    public PythonBoolean containsSubsequence(PythonBytes subsequence) {
        if (subsequence.value.length == 0) {
            return PythonBoolean.TRUE;
        }

        if (subsequence.value.length > value.length) {
            return PythonBoolean.FALSE;
        }

        for (int i = 0; i <= value.length - subsequence.value.length; i++) {
            if (Arrays.equals(value, i, i + subsequence.value.length,
                    subsequence.value, 0, subsequence.value.length)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBytes concat(PythonBytes other) {
        if (value.length == 0) {
            return other;
        } else if (other.value.length == 0) {
            return this;
        } else {
            byte[] out = new byte[value.length + other.value.length];
            System.arraycopy(value, 0, out, 0, value.length);
            System.arraycopy(other.value, 0, out, value.length, other.value.length);
            return new PythonBytes(out);
        }
    }

    public PythonBytes repeat(PythonInteger times) {
        int timesAsInt = times.value.intValueExact();

        if (timesAsInt <= 0) {
            return EMPTY;
        }

        if (timesAsInt == 1 || value.length == 0) {
            return this;
        }

        byte[] out = new byte[value.length * timesAsInt];
        for (int i = 0; i < timesAsInt; i++) {
            System.arraycopy(value, 0, out, i * value.length, value.length);
        }
        return new PythonBytes(out);
    }

    public DelegatePythonIterator<PythonInteger> getIterator() {
        return new DelegatePythonIterator<>(IntStream.range(0, value.length)
                .mapToObj(index -> BYTE_TO_INT[Byte.toUnsignedInt(value[index])])
                .iterator());
    }

    public PythonInteger countByte(byte query, int start, int end) {
        int count = 0;
        for (int i = start; i < end; i++) {
            if (value[i] == query) {
                count++;
            }
        }
        return PythonInteger.valueOf(count);
    }

    public PythonInteger count(PythonInteger byteAsInt) {
        byte query = byteAsInt.asByte();
        return countByte(query, 0, value.length);
    }

    public PythonInteger count(PythonInteger byteAsInt, PythonInteger start) {
        byte query = byteAsInt.asByte();
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);

        return countByte(query, startAsInt, value.length);
    }

    public PythonInteger count(PythonInteger byteAsInt, PythonInteger start, PythonInteger end) {
        byte query = byteAsInt.asByte();
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

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
            if (Arrays.equals(value, i, i + query.length,
                    query, 0, query.length)) {
                count++;
                i += query.length - 1;
            }
        }
        return PythonInteger.valueOf(count);
    }

    public PythonInteger count(PythonBytes bytes) {
        return countSubsequence(bytes.value, 0, value.length);
    }

    public PythonInteger count(PythonBytes bytes, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);

        return countSubsequence(bytes.value, startAsInt, value.length);
    }

    public PythonInteger count(PythonBytes bytes, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return countSubsequence(bytes.value, startAsInt, endAsInt);
    }

    public boolean hasPrefix(byte[] prefixBytes, int start, int end) {
        if (prefixBytes.length > end - start) {
            return false;
        }

        for (int i = 0; i < prefixBytes.length; i++) {
            if (prefixBytes[i] != value[i + start]) {
                return false;
            }
        }

        return true;
    }

    public boolean hasSuffix(byte[] suffixBytes, int start, int end) {
        if (suffixBytes.length > end - start) {
            return false;
        }

        for (int i = 1; i <= suffixBytes.length; i++) {
            if (suffixBytes[suffixBytes.length - i] != value[end - i]) {
                return false;
            }
        }

        return true;
    }

    public PythonBytes removePrefix(PythonBytes prefix) {
        byte[] prefixBytes = prefix.value;

        return hasPrefix(prefixBytes, 0, value.length)
                ? new PythonBytes(Arrays.copyOfRange(value, prefixBytes.length, value.length))
                : this;
    }

    public PythonBytes removeSuffix(PythonBytes suffix) {
        byte[] suffixBytes = suffix.value;

        return hasSuffix(suffixBytes, 0, value.length)
                ? new PythonBytes(Arrays.copyOfRange(value, 0, value.length - suffixBytes.length))
                : this;
    }

    public PythonString decode() {
        try {
            return PythonString.valueOf(StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value)).toString());
        } catch (CharacterCodingException e) {
            throw new UnicodeDecodeError(e.getMessage());
        }

    }

    public PythonString decode(PythonString charset) {
        try {
            return PythonString.valueOf(Charset.forName(charset.value).newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value)).toString());
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
                    .decode(ByteBuffer.wrap(value)).toString());
        } catch (CharacterCodingException e) {
            throw new UnicodeDecodeError(e.getMessage());
        }
    }

    public PythonBoolean endsWith(PythonBytes suffix) {
        return PythonBoolean.valueOf(hasSuffix(suffix.value, 0, value.length));
    }

    public PythonBoolean endsWith(PythonBytes suffix, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return PythonBoolean.valueOf(hasSuffix(suffix.value, startAsInt, value.length));
    }

    public PythonBoolean endsWith(PythonBytes suffix, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);
        return PythonBoolean.valueOf(hasSuffix(suffix.value, startAsInt, endAsInt));
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonBytes> suffixes) {
        for (PythonBytes suffix : suffixes) {
            if (hasSuffix(suffix.value, 0, value.length)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonBytes> suffixes, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);

        for (PythonBytes suffix : suffixes) {
            if (hasSuffix(suffix.value, startAsInt, value.length)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonBytes> suffixes, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        for (PythonBytes suffix : suffixes) {
            if (hasSuffix(suffix.value, startAsInt, endAsInt)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    private PythonInteger find(PythonInteger query, int start, int end) {
        byte queryByte = query.asByte();

        for (int i = start; i < end; i++) {
            if (value[i] == queryByte) {
                return PythonInteger.valueOf(i);
            }
        }

        return PythonInteger.valueOf(-1);
    }

    private PythonInteger index(PythonInteger query, int start, int end) {
        byte queryByte = query.asByte();

        for (int i = start; i < end; i++) {
            if (value[i] == queryByte) {
                return PythonInteger.valueOf(i);
            }
        }

        throw new ValueError("Subsequence not found");
    }

    public PythonInteger find(PythonInteger query) {
        return find(query, 0, value.length);
    }

    public PythonInteger find(PythonInteger query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return find(query, startAsInt, value.length);
    }

    public PythonInteger find(PythonInteger query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return find(query, startAsInt, endAsInt);
    }

    public PythonInteger index(PythonInteger query) {
        return index(query, 0, value.length);
    }

    public PythonInteger index(PythonInteger query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return index(query, startAsInt, value.length);
    }

    public PythonInteger index(PythonInteger query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return index(query, startAsInt, endAsInt);
    }

    private PythonInteger find(PythonBytesLikeObject query, int start, int end) {
        byte[] queryBytes = query.asByteArray();

        if (queryBytes.length == 0) {
            return (value.length > 0) ? PythonInteger.ZERO : PythonInteger.valueOf(-1);
        }

        for (int i = start; i <= end - queryBytes.length; i++) {
            if (Arrays.equals(value, i, i + queryBytes.length, queryBytes, 0, queryBytes.length)) {
                return PythonInteger.valueOf(i);
            }
        }

        return PythonInteger.valueOf(-1);
    }

    private PythonInteger index(PythonBytesLikeObject query, int start, int end) {
        byte[] queryBytes = query.asByteArray();

        if (queryBytes.length == 0) {
            if (value.length > 0) {
                return PythonInteger.ZERO;
            } else {
                throw new ValueError("Subsequence not found");
            }
        }

        for (int i = start; i <= end - queryBytes.length; i++) {
            if (Arrays.equals(value, i, i + queryBytes.length, queryBytes, 0, queryBytes.length)) {
                return PythonInteger.valueOf(i);
            }
        }

        throw new ValueError("Subsequence not found");
    }

    public PythonInteger find(PythonBytes query) {
        return find(query, 0, value.length);
    }

    public PythonInteger find(PythonBytes query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return find(query, startAsInt, value.length);
    }

    public PythonInteger find(PythonBytes query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return find(query, startAsInt, endAsInt);
    }

    public PythonInteger index(PythonBytes query) {
        return index(query, 0, value.length);
    }

    public PythonInteger index(PythonBytes query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return index(query, startAsInt, value.length);
    }

    public PythonInteger index(PythonBytes query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return index(query, startAsInt, endAsInt);
    }

    public PythonBytes interpolate(PythonLikeObject object) {
        if (object instanceof PythonLikeTuple) {
            return interpolate((PythonLikeTuple) object);
        } else if (object instanceof PythonLikeDict) {
            return interpolate((PythonLikeDict) object);
        } else {
            return interpolate(PythonLikeTuple.fromItems(object));
        }
    }

    public PythonBytes interpolate(PythonLikeTuple tuple) {
        return PythonString.valueOf(StringFormatter.printfInterpolate(asCharSequence(), tuple,
                StringFormatter.PrintfStringType.BYTES)).asAsciiBytes();
    }

    public PythonBytes interpolate(PythonLikeDict dict) {
        return PythonString.valueOf(StringFormatter.printfInterpolate(asCharSequence(), dict,
                StringFormatter.PrintfStringType.BYTES)).asAsciiBytes();
    }

    public PythonBytes join(PythonLikeObject iterable) {
        PythonIterator<?> iterator = (PythonIterator<?>) UnaryDunderBuiltin.ITERATOR.invoke(iterable);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (iterator.hasNext()) {
            PythonLikeObject item = iterator.nextPythonItem();

            if (!(item instanceof PythonBytesLikeObject)) {
                throw new TypeError("type " + item.$getType() + " is not a bytes-like type");
            }

            outputStream.writeBytes(((PythonBytesLikeObject) item).asByteArray());
            if (iterator.hasNext()) {
                outputStream.writeBytes(value);
            }
        }
        return new PythonBytes(outputStream.toByteArray());
    }

    private PythonLikeTuple partition(PythonBytesLikeObject sep, int start, int end) {
        byte[] sepBytes = sep.asByteArray();

        for (int i = start; i < end - sepBytes.length; i++) {
            int j = 0;
            for (; j < sepBytes.length; j++) {
                if (value[i + j] != sepBytes[j]) {
                    break;
                }
            }

            if (j == sepBytes.length) {
                return PythonLikeTuple.fromItems(
                        new PythonBytes(Arrays.copyOfRange(value, 0, i)),
                        sep,
                        new PythonBytes(Arrays.copyOfRange(value, i + sepBytes.length, value.length)));
            }
        }

        return PythonLikeTuple.fromItems(
                this,
                EMPTY,
                EMPTY);
    }

    public PythonLikeTuple partition(PythonBytes sep) {
        return partition(sep, 0, value.length);
    }

    public PythonLikeTuple partition(PythonBytes sep, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);

        return partition(sep, startAsInt, value.length);
    }

    public PythonLikeTuple partition(PythonBytes sep, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidStartIntIndexForLength(end, value.length);

        return partition(sep, startAsInt, endAsInt);
    }

    public PythonBytes replace(PythonBytesLikeObject old, PythonBytesLikeObject replacement) {
        byte[] oldBytes = old.asByteArray();
        byte[] replacementBytes = replacement.asByteArray();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int lastReplacementEnd = 0;
        for (int i = 0; i < value.length - oldBytes.length; i++) {
            if (!Arrays.equals(value, i, i + oldBytes.length,
                    oldBytes, 0, oldBytes.length)) {
                continue;
            }

            outputStream.write(value, lastReplacementEnd, i - lastReplacementEnd);
            outputStream.writeBytes(replacementBytes);

            i += oldBytes.length;
            lastReplacementEnd = i;
        }

        outputStream.write(value, lastReplacementEnd, value.length - lastReplacementEnd);
        return new PythonBytes(outputStream.toByteArray());
    }

    public PythonBytes replace(PythonBytesLikeObject old, PythonBytesLikeObject replacement, BigInteger count) {
        byte[] oldBytes = old.asByteArray();
        byte[] replacementBytes = replacement.asByteArray();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int lastReplacementEnd = 0;
        for (int i = 0; i < value.length - oldBytes.length; i++) {
            if (count.compareTo(BigInteger.ZERO) == 0) {
                break;
            }

            if (!Arrays.equals(value, i, i + oldBytes.length,
                    oldBytes, 0, oldBytes.length)) {
                continue;
            }

            outputStream.write(value, lastReplacementEnd, i - lastReplacementEnd);
            outputStream.writeBytes(replacementBytes);

            i += oldBytes.length;
            lastReplacementEnd = i;
            count = count.subtract(BigInteger.ONE);
        }

        outputStream.write(value, lastReplacementEnd, value.length - lastReplacementEnd);
        return new PythonBytes(outputStream.toByteArray());
    }

    public PythonBytes replace(PythonBytes old, PythonBytes replacement) {
        return replace((PythonBytesLikeObject) old, replacement);
    }

    public PythonBytes replace(PythonBytes old, PythonBytes replacement, PythonInteger count) {
        return replace(old, replacement, count.value);
    }

    private PythonInteger rightFind(PythonInteger query, int start, int end) {
        byte queryByte = query.asByte();

        for (int i = end - 1; i >= start; i--) {
            if (value[i] == queryByte) {
                return PythonInteger.valueOf(i);
            }
        }

        return PythonInteger.valueOf(-1);
    }

    private PythonInteger rightIndex(PythonInteger query, int start, int end) {
        byte queryByte = query.asByte();

        for (int i = end - 1; i >= start; i--) {
            if (value[i] == queryByte) {
                return PythonInteger.valueOf(i);
            }
        }

        throw new ValueError("Subsequence not found");
    }

    public PythonInteger rightFind(PythonInteger query) {
        return rightFind(query, 0, value.length);
    }

    public PythonInteger rightFind(PythonInteger query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return rightFind(query, startAsInt, value.length);
    }

    public PythonInteger rightFind(PythonInteger query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return rightFind(query, startAsInt, endAsInt);
    }

    public PythonInteger rightIndex(PythonInteger query) {
        return rightIndex(query, 0, value.length);
    }

    public PythonInteger rightIndex(PythonInteger query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return rightIndex(query, startAsInt, value.length);
    }

    public PythonInteger rightIndex(PythonInteger query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return rightIndex(query, startAsInt, endAsInt);
    }

    private PythonInteger rightFind(PythonBytesLikeObject query, int start, int end) {
        byte[] queryBytes = query.asByteArray();

        for (int i = end - queryBytes.length; i >= start; i--) {
            int j = 0;
            for (; j < queryBytes.length; j++) {
                if (value[i + j] != queryBytes[j]) {
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
                if (value[i + j] != queryBytes[j]) {
                    break;
                }
            }

            if (j == queryBytes.length) {
                return PythonInteger.valueOf(i);
            }
        }

        throw new ValueError("Subsequence not found");
    }

    public PythonInteger rightFind(PythonBytes query) {
        return rightFind(query, 0, value.length);
    }

    public PythonInteger rightFind(PythonBytes query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return rightFind(query, startAsInt, value.length);
    }

    public PythonInteger rightFind(PythonBytes query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return rightFind(query, startAsInt, endAsInt);
    }

    public PythonInteger rightIndex(PythonBytes query) {
        return rightIndex(query, 0, value.length);
    }

    public PythonInteger rightIndex(PythonBytes query, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return rightIndex(query, startAsInt, value.length);
    }

    public PythonInteger rightIndex(PythonBytes query, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return rightIndex(query, startAsInt, endAsInt);
    }

    private PythonLikeTuple rightPartition(PythonBytesLikeObject sep, int start, int end) {
        byte[] sepBytes = sep.asByteArray();

        for (int i = end - sepBytes.length; i >= start; i--) {
            if (!Arrays.equals(value, i, i + sepBytes.length,
                    sepBytes, 0, sepBytes.length)) {
                continue;
            }

            return PythonLikeTuple.fromItems(
                    new PythonBytes(Arrays.copyOfRange(value, 0, i)),
                    sep,
                    new PythonBytes(Arrays.copyOfRange(value, i + sepBytes.length, value.length)));
        }

        return PythonLikeTuple.fromItems(
                EMPTY,
                EMPTY,
                this);
    }

    public PythonLikeTuple rightPartition(PythonBytes sep) {
        return rightPartition(sep, 0, value.length);
    }

    public PythonBoolean startsWith(PythonBytes prefix) {
        return PythonBoolean.valueOf(hasPrefix(prefix.value, 0, value.length));
    }

    public PythonBoolean startsWith(PythonBytes prefix, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        return PythonBoolean.valueOf(hasPrefix(prefix.value, startAsInt, value.length));
    }

    public PythonBoolean startsWith(PythonBytes prefix, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        return PythonBoolean.valueOf(hasPrefix(prefix.value, startAsInt, endAsInt));
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonBytes> prefixes) {
        for (PythonBytes prefix : prefixes) {
            if (hasPrefix(prefix.value, 0, value.length)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonBytes> prefixes, PythonInteger start) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);

        for (PythonBytes prefix : prefixes) {
            if (hasPrefix(prefix.value, startAsInt, value.length)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonBytes> prefixes, PythonInteger start, PythonInteger end) {
        int startAsInt = PythonSlice.asValidStartIntIndexForLength(start, value.length);
        int endAsInt = PythonSlice.asValidEndIntIndexForLength(end, value.length);

        for (PythonBytes prefix : prefixes) {
            if (hasPrefix(prefix.value, startAsInt, endAsInt)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBytes translate(PythonBytes table) {
        byte[] tableBytes = table.value;
        if (tableBytes.length != 256) {
            throw new ValueError("translate table must be a bytes object of length 256");
        }

        byte[] out = new byte[value.length];

        for (int i = 0; i < value.length; i++) {
            out[i] = tableBytes[value[i] & 0xFF];
        }

        return new PythonBytes(out);
    }

    public PythonBytes translate(PythonNone table) {
        return this;
    }

    public PythonBytes translate(PythonBytes table, PythonBytes delete) {
        byte[] tableBytes = table.value;
        if (tableBytes.length != 256) {
            throw new ValueError("translate table must be a bytes object of length 256");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length);
        BitSet removedSet = asBitSet(delete);

        for (byte b : value) {
            if (!removedSet.get(b & 0xFF)) {
                out.write(tableBytes, b & 0xFF, 1);
            }
        }

        return new PythonBytes(out.toByteArray());
    }

    public PythonBytes translate(PythonNone table, PythonBytes delete) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length);
        BitSet removedSet = asBitSet(delete);

        for (int i = 0; i < value.length; i++) {
            if (!removedSet.get(value[i] & 0xFF)) {
                out.write(value, i, 1);
            }
        }

        return new PythonBytes(out.toByteArray());
    }

    public PythonBytes center(PythonInteger fillWidth) {
        return center(fillWidth, ASCII_SPACE);
    }

    public PythonBytes center(PythonInteger fillWidth, PythonBytes fillCharacter) {
        if (fillCharacter.value.length != 1) {
            throw new TypeError("center() argument 2 must be a byte string of length 1");
        }

        int widthAsInt = fillWidth.value.intValueExact();
        if (widthAsInt <= value.length) {
            return this;
        }
        int extraWidth = widthAsInt - value.length;
        int rightPadding = extraWidth / 2;
        // left padding get extra character if extraWidth is odd
        int leftPadding = rightPadding + (extraWidth & 1); // x & 1 == x % 2

        byte[] out = new byte[widthAsInt];
        Arrays.fill(out, 0, leftPadding, fillCharacter.value[0]);
        System.arraycopy(value, 0, out, leftPadding, value.length);
        Arrays.fill(out, leftPadding + value.length, widthAsInt, fillCharacter.value[0]);

        return new PythonBytes(out);
    }

    public PythonBytes leftJustify(PythonInteger fillWidth) {
        return leftJustify(fillWidth, ASCII_SPACE);
    }

    public PythonBytes leftJustify(PythonInteger fillWidth, PythonBytes fillCharacter) {
        if (fillCharacter.value.length != 1) {
            throw new TypeError("ljust() argument 2 must be a byte string of length 1");
        }

        int widthAsInt = fillWidth.value.intValueExact();
        if (widthAsInt <= value.length) {
            return this;
        }

        byte[] out = new byte[widthAsInt];
        System.arraycopy(value, 0, out, 0, value.length);
        Arrays.fill(out, value.length, widthAsInt, fillCharacter.value[0]);

        return new PythonBytes(out);
    }

    public PythonBytes rightJustify(PythonInteger fillWidth) {
        return rightJustify(fillWidth, ASCII_SPACE);
    }

    public PythonBytes rightJustify(PythonInteger fillWidth, PythonBytes fillCharacter) {
        if (fillCharacter.value.length != 1) {
            throw new TypeError("rjust() argument 2 must be a byte string of length 1");
        }

        int widthAsInt = fillWidth.value.intValueExact();
        if (widthAsInt <= value.length) {
            return this;
        }

        int extraWidth = widthAsInt - value.length;

        byte[] out = new byte[widthAsInt];
        Arrays.fill(out, 0, extraWidth, fillCharacter.value[0]);
        System.arraycopy(value, 0, out, extraWidth, value.length);

        return new PythonBytes(out);
    }

    public PythonBytes strip() {
        return strip(ASCII_SPACE);
    }

    public PythonBytes strip(PythonNone ignored) {
        return strip();
    }

    public PythonBytes strip(PythonBytes bytesToStrip) {
        BitSet toStrip = asBitSet(bytesToStrip);

        int start = 0;
        int end = value.length - 1;

        while (start < value.length && toStrip.get(value[start] & 0xFF)) {
            start++;
        }

        while (end >= start && toStrip.get(value[end] & 0xFF)) {
            end--;
        }

        if (end < start) {
            return new PythonBytes(new byte[] {});
        }

        return new PythonBytes(Arrays.copyOfRange(value, start, end + 1));
    }

    public PythonBytes leftStrip() {
        return leftStrip(ASCII_SPACE);
    }

    public PythonBytes leftStrip(PythonNone ignored) {
        return leftStrip();
    }

    public PythonBytes leftStrip(PythonBytes bytesToStrip) {
        BitSet toStrip = asBitSet(bytesToStrip);

        int start = 0;

        while (start < value.length && toStrip.get(value[start] & 0xFF)) {
            start++;
        }

        if (start == value.length) {
            return new PythonBytes(new byte[] {});
        }

        return new PythonBytes(Arrays.copyOfRange(value, start, value.length));
    }

    public PythonBytes rightStrip() {
        return rightStrip(ASCII_SPACE);
    }

    public PythonBytes rightStrip(PythonNone ignored) {
        return rightStrip();
    }

    public PythonBytes rightStrip(PythonBytes bytesToStrip) {
        BitSet toStrip = asBitSet(bytesToStrip);

        int end = value.length - 1;

        while (end >= 0 && toStrip.get(value[end] & 0xFF)) {
            end--;
        }

        if (end < 0) {
            return new PythonBytes(new byte[] {});
        }

        return new PythonBytes(Arrays.copyOfRange(value, 0, end + 1));
    }

    public PythonLikeList<PythonBytes> split() {
        PythonLikeList<PythonBytes> out = new PythonLikeList<>();
        int start = 0;
        int end = value.length;

        while (end > 0 && ASCII_WHITESPACE_BITSET.get(value[end - 1] & 0xFF)) {
            end--;
        }

        while (start < end && ASCII_WHITESPACE_BITSET.get(value[start] & 0xFF)) {
            start++;
        }

        if (start == end) {
            return out;
        }

        int lastEnd = start;
        while (start < end - 1) {
            while (start < end - 1 &&
                    !ASCII_WHITESPACE_BITSET.get(value[start] & 0xFF)) {
                start++;
            }
            if (start != end - 1) {
                out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, start)));
                lastEnd = start + 1;
                start = lastEnd;
            }
        }

        if (lastEnd != end) {
            out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, end)));
        }

        return out;
    }

    public PythonLikeList<PythonBytes> split(PythonNone ignored) {
        return split();
    }

    public PythonLikeList<PythonBytes> split(PythonBytes seperator) {
        PythonLikeList<PythonBytes> out = new PythonLikeList<>();
        int start = 0;
        int end = value.length;

        int lastEnd = start;
        while (start < end - seperator.value.length) {
            while (start < end - seperator.value.length &&
                    !Arrays.equals(value, start, start + seperator.value.length,
                            seperator.value, 0, seperator.value.length)) {
                start++;
            }
            if (start != end - seperator.value.length) {
                out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, start)));
                lastEnd = start + seperator.value.length;
                start = lastEnd;
            }
        }

        if (Arrays.equals(value, start, start + seperator.value.length,
                seperator.value, 0, seperator.value.length)) {
            out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, start)));
            lastEnd = start + seperator.value.length;
        }

        out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, end)));
        return out;
    }

    public PythonLikeList<PythonBytes> split(PythonBytes seperator, PythonInteger maxSplits) {
        if (maxSplits.equals(new PythonInteger(-1))) {
            return split(seperator);
        }

        PythonLikeList<PythonBytes> out = new PythonLikeList<>();
        int start = 0;
        int end = value.length;

        int lastEnd = start;
        while (start < end - seperator.value.length && maxSplits.compareTo(PythonInteger.ONE) >= 0) {
            while (start < end - seperator.value.length &&
                    !Arrays.equals(value, start, start + seperator.value.length,
                            seperator.value, 0, seperator.value.length)) {
                start++;
            }
            if (start != end - seperator.value.length) {
                out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, start)));
                lastEnd = start + seperator.value.length;
                start = lastEnd;
                maxSplits = maxSplits.subtract(PythonInteger.ONE);
            }
        }

        if (maxSplits.compareTo(PythonInteger.ONE) >= 0 &&
                Arrays.equals(value, start, start + seperator.value.length,
                        seperator.value, 0, seperator.value.length)) {
            out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, start)));
            lastEnd = start + seperator.value.length;
        }

        out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, end)));
        return out;
    }

    public PythonLikeList<PythonBytes> split(PythonNone seperator, PythonInteger maxSplits) {
        if (maxSplits.equals(new PythonInteger(-1))) {
            return split(seperator);
        }

        PythonLikeList<PythonBytes> out = new PythonLikeList<>();
        int start = 0;
        int end = value.length;

        while (end > 0 && ASCII_WHITESPACE_BITSET.get(value[end - 1] & 0xFF)) {
            end--;
        }

        while (start < end && ASCII_WHITESPACE_BITSET.get(value[start] & 0xFF)) {
            start++;
        }

        if (start == end) {
            return out;
        }

        int lastEnd = start;
        while (start < end - 1 && maxSplits.compareTo(PythonInteger.ONE) >= 0) {
            while (start < end - 1 && !ASCII_WHITESPACE_BITSET.get(value[start] & 0xFF)) {
                start++;
            }
            if (start != end - 1) {
                out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, start)));
                lastEnd = start + 1;
                start = lastEnd;
                maxSplits = maxSplits.subtract(PythonInteger.ONE);
            }
        }

        if (lastEnd != end) {
            out.add(new PythonBytes(Arrays.copyOfRange(value, lastEnd, end)));
        }

        return out;
    }

    public PythonLikeList<PythonBytes> rightSplit() {
        return split();
    }

    public PythonLikeList<PythonBytes> rightSplit(PythonNone ignored) {
        return rightSplit();
    }

    public PythonLikeList<PythonBytes> rightSplit(PythonBytes seperator) {
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

    public PythonLikeList<PythonBytes> rightSplit(PythonBytes seperator, PythonInteger maxSplits) {
        if (maxSplits.equals(new PythonInteger(-1))) {
            return split(seperator);
        }

        PythonLikeList<PythonBytes> out = new PythonLikeList<>();

        byte[] reversedValue = reverseInplace(value.clone());
        byte[] reversedSep = reverseInplace(seperator.value.clone());

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
                out.add(new PythonBytes(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, start))));
                lastEnd = start + reversedSep.length;
                start = lastEnd;
                maxSplits = maxSplits.subtract(PythonInteger.ONE);
            }
        }

        if (maxSplits.compareTo(PythonInteger.ONE) >= 0 &&
                Arrays.equals(reversedValue, start, start + reversedSep.length,
                        reversedSep, 0, reversedSep.length)) {
            out.add(new PythonBytes(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, start))));
            lastEnd = start + seperator.value.length;
        }

        out.add(new PythonBytes(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, end))));
        out.reverse();
        return out;
    }

    public PythonLikeList<PythonBytes> rightSplit(PythonNone seperator, PythonInteger maxSplits) {
        if (maxSplits.equals(new PythonInteger(-1))) {
            return split(seperator);
        }

        PythonLikeList<PythonBytes> out = new PythonLikeList<>();

        byte[] reversedValue = reverseInplace(value.clone());

        int start = 0;
        int end = value.length;

        while (end > 0 && ASCII_WHITESPACE_BITSET.get(value[end - 1] & 0xFF)) {
            end--;
        }

        while (start < end && ASCII_WHITESPACE_BITSET.get(value[start] & 0xFF)) {
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
                out.add(new PythonBytes(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, start))));
                lastEnd = start + 1;
                start = lastEnd;
                maxSplits = maxSplits.subtract(PythonInteger.ONE);
            }
        }

        if (lastEnd != end) {
            out.add(new PythonBytes(reverseInplace(Arrays.copyOfRange(reversedValue, lastEnd, end))));
        }

        out.reverse();
        return out;
    }

    public PythonBytes capitalize() {
        var asString = asAsciiString();
        if (asString.value.isEmpty()) {
            return this;
        }
        var tail = PythonString.valueOf(asString.value.substring(1))
                .withModifiedCodepoints(cp -> cp < 128 ? Character.toLowerCase(cp) : cp).value;
        var head = asString.value.charAt(0);
        if (head < 128) {
            head = Character.toTitleCase(head);
        }
        return (PythonString.valueOf(head + tail)).asAsciiBytes();
    }

    public PythonBytes expandTabs() {
        return asAsciiString().expandTabs().asAsciiBytes();
    }

    public PythonBytes expandTabs(PythonInteger tabSize) {
        return asAsciiString().expandTabs(tabSize).asAsciiBytes();
    }

    public PythonBoolean isAlphaNumeric() {
        return asAsciiString().isAlphaNumeric();
    }

    public PythonBoolean isAlpha() {
        return asAsciiString().isAlpha();
    }

    public PythonBoolean isAscii() {
        for (byte b : value) {
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

    public PythonBytes lower() {
        return asAsciiString().withModifiedCodepoints(
                cp -> cp < 128 ? Character.toLowerCase(cp) : cp).asAsciiBytes();
    }

    public PythonLikeList<PythonBytes> splitLines() {
        return asAsciiString().splitLines()
                .stream()
                .map(PythonString::asAsciiBytes)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonBytes> splitLines(PythonBoolean keepEnds) {
        return asAsciiString().splitLines(keepEnds)
                .stream()
                .map(PythonString::asAsciiBytes)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonBytes swapCase() {
        return asAsciiString().withModifiedCodepoints(
                cp -> cp < 128 ? PythonString.CharacterCase.swapCase(cp) : cp).asAsciiBytes();
    }

    public PythonBytes title() {
        return asAsciiString().title(cp -> cp < 128).asAsciiBytes();
    }

    public PythonBytes upper() {
        return asAsciiString().withModifiedCodepoints(
                cp -> cp < 128 ? Character.toUpperCase(cp) : cp).asAsciiBytes();
    }

    public PythonBytes zfill(PythonInteger width) {
        return asAsciiString().zfill(width).asAsciiBytes();
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
        boolean hasSingleQuotes = false;
        boolean hasDoubleQuotes = false;

        for (byte b : value) {
            switch (b) {
                case '\'':
                    hasSingleQuotes = true;
                    break;

                case '\"':
                    hasDoubleQuotes = true;
                    break;

                // Default: do nothing
            }
        }

        StringBuilder out = new StringBuilder(value.length);
        out.append("b");

        if (!hasSingleQuotes || hasDoubleQuotes) {
            out.append('\'');
        } else {
            out.append('\"');
        }

        boolean escapeSingleQuotes = hasSingleQuotes && hasDoubleQuotes;
        for (byte b : value) {
            switch (b) {
                case '\'':
                    if (escapeSingleQuotes) {
                        out.append("\\'");
                    } else {
                        out.append('\'');
                    }
                    break;

                case '\\':
                    out.append("\\\\");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    out.append((char) (b & 0xFF));
                    break;
            }
        }

        if (!hasSingleQuotes || hasDoubleQuotes) {
            out.append('\'');
        } else {
            out.append('\"');
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
        PythonBytes that = (PythonBytes) o;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
