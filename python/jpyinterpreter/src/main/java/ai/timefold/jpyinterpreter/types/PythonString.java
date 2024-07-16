package ai.timefold.jpyinterpreter.types;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.builtins.BinaryDunderBuiltin;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.collections.DelegatePythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeList;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.errors.lookup.IndexError;
import ai.timefold.jpyinterpreter.types.errors.lookup.LookupError;
import ai.timefold.jpyinterpreter.types.errors.unicode.UnicodeDecodeError;
import ai.timefold.jpyinterpreter.types.errors.unicode.UnicodeEncodeError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.util.DefaultFormatSpec;
import ai.timefold.jpyinterpreter.util.StringFormatter;
import ai.timefold.jpyinterpreter.util.arguments.ArgumentSpec;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

public class PythonString extends AbstractPythonLikeObject implements PythonLikeComparable<PythonString>, PlanningImmutable {
    public final String value;

    public final static PythonString EMPTY = new PythonString("");

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonString::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        PythonLikeComparable.setup(BuiltinTypes.STRING_TYPE);
        BuiltinTypes.STRING_TYPE.setConstructor((positionalArguments, namedArguments, callerInstance) -> {
            if (positionalArguments.size() == 1) { // TODO: Named arguments
                return UnaryDunderBuiltin.STR.invoke(positionalArguments.get(0));
            } else if (positionalArguments.size() == 2) {
                if (!(positionalArguments.get(0) instanceof PythonBytes) ||
                        !(positionalArguments.get(1) instanceof PythonString)) {
                    throw new TypeError(); // TODO: Better error message
                }
                return ((PythonBytes) positionalArguments.get(0)).decode((PythonString) positionalArguments.get(1));
            } else if (positionalArguments.size() == 3) {
                if (!(positionalArguments.get(0) instanceof PythonBytes) ||
                        !(positionalArguments.get(1) instanceof PythonString) ||
                        !(positionalArguments.get(2) instanceof PythonString)) {
                    throw new TypeError(); // TODO: Better error message
                }
                return ((PythonBytes) positionalArguments.get(0)).decode((PythonString) positionalArguments.get(1),
                        (PythonString) positionalArguments.get(2));
            } else {
                throw new ValueError("str expects 1 or 3 arguments, got " + positionalArguments.size());
            }
        });

        // Unary
        BuiltinTypes.STRING_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION, PythonString.class.getMethod("repr"));
        BuiltinTypes.STRING_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING, PythonString.class.getMethod("asString"));
        BuiltinTypes.STRING_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR, PythonString.class.getMethod("getIterator"));
        BuiltinTypes.STRING_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, PythonString.class.getMethod("getLength"));

        // Binary
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonString.class.getMethod("getCharAt", PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonString.class.getMethod("getSubstring", PythonSlice.class));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.CONTAINS,
                PythonString.class.getMethod("containsSubstring", PythonString.class));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.ADD,
                PythonString.class.getMethod("concat", PythonString.class));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.MULTIPLY,
                PythonString.class.getMethod("repeat", PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonString.class.getMethod("interpolate", PythonLikeObject.class));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonString.class.getMethod("interpolate", PythonLikeTuple.class));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.MODULO,
                PythonString.class.getMethod("interpolate", PythonLikeDict.class));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.FORMAT, PythonString.class.getMethod("formatSelf"));
        BuiltinTypes.STRING_TYPE.addBinaryMethod(PythonBinaryOperator.FORMAT,
                PythonString.class.getMethod("formatSelf", PythonString.class));

        // Other
        BuiltinTypes.STRING_TYPE.addMethod("capitalize", PythonString.class.getMethod("capitalize"));
        BuiltinTypes.STRING_TYPE.addMethod("casefold", PythonString.class.getMethod("casefold"));

        BuiltinTypes.STRING_TYPE.addMethod("center", PythonString.class.getMethod("center", PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("center",
                PythonString.class.getMethod("center", PythonInteger.class, PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("count", PythonString.class.getMethod("count", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("count",
                PythonString.class.getMethod("count", PythonString.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("count",
                PythonString.class.getMethod("count", PythonString.class, PythonInteger.class, PythonInteger.class));

        // TODO: encode

        BuiltinTypes.STRING_TYPE.addMethod("endswith", PythonString.class.getMethod("endsWith", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("endswith", PythonString.class.getMethod("endsWith", PythonLikeTuple.class));
        BuiltinTypes.STRING_TYPE.addMethod("endswith",
                PythonString.class.getMethod("endsWith", PythonString.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("endswith",
                PythonString.class.getMethod("endsWith", PythonLikeTuple.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("endswith",
                PythonString.class.getMethod("endsWith", PythonString.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("endswith",
                PythonString.class.getMethod("endsWith", PythonLikeTuple.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("expandtabs", PythonString.class.getMethod("expandTabs"));
        BuiltinTypes.STRING_TYPE.addMethod("expandtabs", PythonString.class.getMethod("expandTabs", PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("find", PythonString.class.getMethod("findSubstringIndex", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("find",
                PythonString.class.getMethod("findSubstringIndex", PythonString.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("find", PythonString.class.getMethod("findSubstringIndex", PythonString.class,
                PythonInteger.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("format", ArgumentSpec.forFunctionReturning("format", PythonString.class.getName())
                .addExtraPositionalVarArgument("vargs")
                .addExtraKeywordVarArgument("kwargs")
                .asPythonFunctionSignature(PythonString.class.getMethod("format", List.class, Map.class)));

        BuiltinTypes.STRING_TYPE.addMethod("format_map", PythonString.class.getMethod("formatMap", PythonLikeDict.class));

        BuiltinTypes.STRING_TYPE.addMethod("index",
                PythonString.class.getMethod("findSubstringIndexOrError", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("index",
                PythonString.class.getMethod("findSubstringIndexOrError", PythonString.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("index",
                PythonString.class.getMethod("findSubstringIndexOrError", PythonString.class,
                        PythonInteger.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("isalnum", PythonString.class.getMethod("isAlphaNumeric"));
        BuiltinTypes.STRING_TYPE.addMethod("isalpha", PythonString.class.getMethod("isAlpha"));
        BuiltinTypes.STRING_TYPE.addMethod("isascii", PythonString.class.getMethod("isAscii"));
        BuiltinTypes.STRING_TYPE.addMethod("isdecimal", PythonString.class.getMethod("isDecimal"));
        BuiltinTypes.STRING_TYPE.addMethod("isdigit", PythonString.class.getMethod("isDigit"));
        BuiltinTypes.STRING_TYPE.addMethod("isidentifier", PythonString.class.getMethod("isIdentifier"));
        BuiltinTypes.STRING_TYPE.addMethod("islower", PythonString.class.getMethod("isLower"));
        BuiltinTypes.STRING_TYPE.addMethod("isnumeric", PythonString.class.getMethod("isNumeric"));
        BuiltinTypes.STRING_TYPE.addMethod("isprintable", PythonString.class.getMethod("isPrintable"));
        BuiltinTypes.STRING_TYPE.addMethod("isspace", PythonString.class.getMethod("isSpace"));
        BuiltinTypes.STRING_TYPE.addMethod("istitle", PythonString.class.getMethod("isTitle"));
        BuiltinTypes.STRING_TYPE.addMethod("isupper", PythonString.class.getMethod("isUpper"));

        BuiltinTypes.STRING_TYPE.addMethod("join", PythonString.class.getMethod("join", PythonLikeObject.class));

        BuiltinTypes.STRING_TYPE.addMethod("ljust", PythonString.class.getMethod("leftJustify", PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("ljust",
                PythonString.class.getMethod("leftJustify", PythonInteger.class, PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("lower", PythonString.class.getMethod("lower"));

        BuiltinTypes.STRING_TYPE.addMethod("lstrip", PythonString.class.getMethod("leftStrip"));
        BuiltinTypes.STRING_TYPE.addMethod("lstrip", PythonString.class.getMethod("leftStrip", PythonNone.class));
        BuiltinTypes.STRING_TYPE.addMethod("lstrip", PythonString.class.getMethod("leftStrip", PythonString.class));

        // TODO: maketrans

        BuiltinTypes.STRING_TYPE.addMethod("partition", PythonString.class.getMethod("partition", PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("removeprefix", PythonString.class.getMethod("removePrefix", PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("removesuffix", PythonString.class.getMethod("removeSuffix", PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("replace",
                PythonString.class.getMethod("replaceAll", PythonString.class, PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("replace",
                PythonString.class.getMethod("replaceUpToCount", PythonString.class, PythonString.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("rfind",
                PythonString.class.getMethod("rightFindSubstringIndex", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("rfind",
                PythonString.class.getMethod("rightFindSubstringIndex", PythonString.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("rfind", PythonString.class.getMethod("rightFindSubstringIndex", PythonString.class,
                PythonInteger.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("rindex",
                PythonString.class.getMethod("rightFindSubstringIndexOrError", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("rindex",
                PythonString.class.getMethod("rightFindSubstringIndexOrError", PythonString.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("rindex",
                PythonString.class.getMethod("rightFindSubstringIndexOrError", PythonString.class,
                        PythonInteger.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("rjust", PythonString.class.getMethod("rightJustify", PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("rjust",
                PythonString.class.getMethod("rightJustify", PythonInteger.class, PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("rpartition", PythonString.class.getMethod("rightPartition", PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("rsplit", PythonString.class.getMethod("rightSplit"));
        BuiltinTypes.STRING_TYPE.addMethod("rsplit", PythonString.class.getMethod("rightSplit", PythonNone.class));
        BuiltinTypes.STRING_TYPE.addMethod("rsplit", PythonString.class.getMethod("rightSplit", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("rsplit",
                PythonString.class.getMethod("rightSplit", PythonNone.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("rsplit",
                PythonString.class.getMethod("rightSplit", PythonString.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("rstrip", PythonString.class.getMethod("rightStrip"));
        BuiltinTypes.STRING_TYPE.addMethod("rstrip", PythonString.class.getMethod("rightStrip", PythonNone.class));
        BuiltinTypes.STRING_TYPE.addMethod("rstrip", PythonString.class.getMethod("rightStrip", PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("split", PythonString.class.getMethod("split"));
        BuiltinTypes.STRING_TYPE.addMethod("split", PythonString.class.getMethod("split", PythonNone.class));
        BuiltinTypes.STRING_TYPE.addMethod("split", PythonString.class.getMethod("split", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("split",
                PythonString.class.getMethod("split", PythonNone.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("split",
                PythonString.class.getMethod("split", PythonString.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("splitlines", PythonString.class.getMethod("splitLines"));
        BuiltinTypes.STRING_TYPE.addMethod("splitlines", PythonString.class.getMethod("splitLines", PythonBoolean.class));

        BuiltinTypes.STRING_TYPE.addMethod("startswith", PythonString.class.getMethod("startsWith", PythonString.class));
        BuiltinTypes.STRING_TYPE.addMethod("startswith", PythonString.class.getMethod("startsWith", PythonLikeTuple.class));
        BuiltinTypes.STRING_TYPE.addMethod("startswith",
                PythonString.class.getMethod("startsWith", PythonString.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("startswith",
                PythonString.class.getMethod("startsWith", PythonLikeTuple.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("startswith",
                PythonString.class.getMethod("startsWith", PythonString.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.STRING_TYPE.addMethod("startswith",
                PythonString.class.getMethod("startsWith", PythonLikeTuple.class, PythonInteger.class, PythonInteger.class));

        BuiltinTypes.STRING_TYPE.addMethod("strip", PythonString.class.getMethod("strip"));
        BuiltinTypes.STRING_TYPE.addMethod("strip", PythonString.class.getMethod("strip", PythonNone.class));
        BuiltinTypes.STRING_TYPE.addMethod("strip", PythonString.class.getMethod("strip", PythonString.class));

        BuiltinTypes.STRING_TYPE.addMethod("swapcase", PythonString.class.getMethod("swapCase"));

        BuiltinTypes.STRING_TYPE.addMethod("title", PythonString.class.getMethod("title"));

        BuiltinTypes.STRING_TYPE.addMethod("translate", PythonString.class.getMethod("translate", PythonLikeObject.class));

        BuiltinTypes.STRING_TYPE.addMethod("upper", PythonString.class.getMethod("upper"));

        BuiltinTypes.STRING_TYPE.addMethod("zfill", PythonString.class.getMethod("zfill", PythonInteger.class));

        return BuiltinTypes.STRING_TYPE;
    }

    public PythonString(String value) {
        super(BuiltinTypes.STRING_TYPE);
        this.value = value;
    }

    public static PythonString valueOf(String value) {
        return new PythonString(value);
    }

    public String getValue() {
        return value;
    }

    public final PythonBytes asAsciiBytes() {
        char[] charData = value.toCharArray();
        int length = 0;
        for (char charDatum : charData) {
            if (charDatum < 0xFF) {
                length++;
            } else {
                length += 2;
            }
        }
        byte[] out = new byte[length];

        int outIndex = 0;
        for (char charDatum : charData) {
            if (charDatum < 0xFF) {
                out[outIndex] = (byte) charDatum;
                outIndex++;
            } else {
                out[outIndex] = (byte) ((charDatum & 0xFF00) >> 8);
                outIndex++;
                out[outIndex] = (byte) (charDatum & 0x00FF);
                outIndex++;
            }
        }
        return new PythonBytes(out);
    }

    public final PythonByteArray asAsciiByteArray() {
        return new PythonByteArray(asAsciiBytes().value);
    }

    public PythonBytes encode() {
        try {
            ByteBuffer byteBuffer = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] out = new byte[byteBuffer.limit()];
            byteBuffer.get(out);
            return new PythonBytes(out);
        } catch (CharacterCodingException e) {
            throw new UnicodeEncodeError(e.getMessage());
        }

    }

    public PythonBytes encode(PythonString charset) {
        try {
            ByteBuffer byteBuffer = Charset.forName(charset.value).newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] out = new byte[byteBuffer.limit()];
            byteBuffer.get(out);
            return new PythonBytes(out);
        } catch (CharacterCodingException e) {
            throw new UnicodeDecodeError(e.getMessage());
        }
    }

    public PythonBytes encode(PythonString charset, PythonString errorActionString) {
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
            ByteBuffer byteBuffer = Charset.forName(charset.value).newEncoder()
                    .onMalformedInput(errorAction)
                    .encode(CharBuffer.wrap(value));
            byte[] out = new byte[byteBuffer.limit()];
            byteBuffer.get(out);
            return new PythonBytes(out);
        } catch (CharacterCodingException e) {
            throw new UnicodeDecodeError(e.getMessage());
        }
    }

    public int length() {
        return value.length();
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(value.length());
    }

    public PythonString getCharAt(PythonInteger position) {
        int index = PythonSlice.asIntIndexForLength(position, value.length());

        if (index >= value.length()) {
            throw new IndexError("position " + position + " larger than string length " + value.length());
        } else if (index < 0) {
            throw new IndexError("position " + position + " is less than 0");
        }

        return new PythonString(Character.toString(value.charAt(index)));
    }

    public PythonString getSubstring(PythonSlice slice) {
        int length = value.length();
        int start = slice.getStartIndex(length);
        int stop = slice.getStopIndex(length);
        int step = slice.getStrideLength();

        if (step == 1) {
            if (stop <= start) {
                return PythonString.valueOf("");
            } else {
                return PythonString.valueOf(value.substring(start, stop));
            }
        } else {
            StringBuilder out = new StringBuilder();
            if (step > 0) {
                for (int i = start; i < stop; i += step) {
                    out.append(value.charAt(i));
                }
            } else {
                for (int i = start; i > stop; i += step) {
                    out.append(value.charAt(i));
                }
            }
            return PythonString.valueOf(out.toString());
        }
    }

    public PythonBoolean containsSubstring(PythonString substring) {
        return PythonBoolean.valueOf(value.contains(substring.value));
    }

    public PythonString concat(PythonString other) {
        if (value.isEmpty()) {
            return other;
        } else if (other.value.isEmpty()) {
            return this;
        } else {
            return PythonString.valueOf(value + other.value);
        }
    }

    public PythonString repeat(PythonInteger times) {
        int timesAsInt = times.value.intValueExact();

        if (timesAsInt <= 0) {
            return EMPTY;
        }

        if (timesAsInt == 1) {
            return this;
        }

        return PythonString.valueOf(value.repeat(timesAsInt));
    }

    public DelegatePythonIterator getIterator() {
        return new DelegatePythonIterator(value.chars().mapToObj(charVal -> new PythonString(Character.toString(charVal)))
                .iterator());
    }

    public PythonString capitalize() {
        if (value.isEmpty()) {
            return this;
        }
        return PythonString.valueOf(Character.toTitleCase(value.charAt(0)) + value.substring(1).toLowerCase());
    }

    public PythonString title() {
        return title(ignored -> true);
    }

    public PythonString title(IntPredicate predicate) {
        if (value.isEmpty()) {
            return this;
        }

        int length = value.length();
        boolean previousIsWordBoundary = true;

        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            if (predicate.test(character)) {
                if (previousIsWordBoundary) {
                    out.append(Character.toTitleCase(character));
                } else {
                    out.append(Character.toLowerCase(character));
                }
            } else {
                out.append(character);
            }

            previousIsWordBoundary = !Character.isAlphabetic(character);
        }

        return PythonString.valueOf(out.toString());
    }

    public PythonString casefold() {
        // This will work for the majority of cases, but fail for some cases
        return PythonString.valueOf(value.toUpperCase().toLowerCase());
    }

    public PythonString swapCase() {
        return withModifiedCodepoints(CharacterCase::swapCase);
    }

    public PythonString lower() {
        return PythonString.valueOf(value.toLowerCase());
    }

    public PythonString upper() {
        return PythonString.valueOf(value.toUpperCase());
    }

    public PythonString withModifiedCodepoints(IntUnaryOperator modifier) {
        return PythonString.valueOf(value.codePoints()
                .map(modifier)
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint, StringBuilder::append)
                .toString());
    }

    public PythonString center(PythonInteger width) {
        return center(width, PythonString.valueOf(" "));
    }

    public PythonString center(PythonInteger width, PythonString fillChar) {
        int widthAsInt = width.value.intValueExact();
        if (widthAsInt <= value.length()) {
            return this;
        }
        int extraWidth = widthAsInt - value.length();
        int rightPadding = extraWidth / 2;
        // left padding get extra character if extraWidth is odd
        int leftPadding = rightPadding + (extraWidth & 1); // x & 1 == x % 2

        if (fillChar.value.length() != 1) {
            throw new TypeError("The fill character must be exactly one character long");
        }

        String fillCharAsString = fillChar.value;

        return PythonString.valueOf(fillCharAsString.repeat(leftPadding) +
                value +
                fillCharAsString.repeat(rightPadding));
    }

    public PythonString rightJustify(PythonInteger width) {
        return rightJustify(width, PythonString.valueOf(" "));
    }

    public PythonString rightJustify(PythonInteger width, PythonString fillChar) {
        int widthAsInt = width.value.intValueExact();
        if (widthAsInt <= value.length()) {
            return this;
        }
        int leftPadding = widthAsInt - value.length();

        if (fillChar.value.length() != 1) {
            throw new TypeError("The fill character must be exactly one character long");
        }

        return PythonString.valueOf(fillChar.value.repeat(leftPadding) + value);
    }

    public PythonString leftJustify(PythonInteger width) {
        return leftJustify(width, PythonString.valueOf(" "));
    }

    public PythonString leftJustify(PythonInteger width, PythonString fillChar) {
        int widthAsInt = width.value.intValueExact();
        if (widthAsInt <= value.length()) {
            return this;
        }
        int rightPadding = widthAsInt - value.length();

        if (fillChar.value.length() != 1) {
            throw new TypeError("The fill character must be exactly one character long");
        }

        return PythonString.valueOf(value + fillChar.value.repeat(rightPadding));
    }

    public PythonInteger count(PythonString sub) {
        Matcher matcher = Pattern.compile(Pattern.quote(sub.value)).matcher(value);
        return PythonInteger.valueOf(matcher.results().count());
    }

    public PythonInteger count(PythonString sub, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());

        Matcher matcher = Pattern.compile(Pattern.quote(sub.value)).matcher(value.substring(startIndex));
        return PythonInteger.valueOf(matcher.results().count());
    }

    public PythonInteger count(PythonString sub, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        Matcher matcher = Pattern.compile(Pattern.quote(sub.value)).matcher(value.substring(startIndex, endIndex));
        return PythonInteger.valueOf(matcher.results().count());
    }

    // TODO: encode https://docs.python.org/3/library/stdtypes.html#str.encode

    public PythonBoolean startsWith(PythonString prefix) {
        return PythonBoolean.valueOf(value.startsWith(prefix.value));
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonString> prefixTuple) {
        for (PythonString prefix : prefixTuple) {
            if (value.startsWith(prefix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean startsWith(PythonString prefix, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        return PythonBoolean.valueOf(value.substring(startIndex).startsWith(prefix.value));
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonString> prefixTuple, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        String toCheck = value.substring(startIndex);
        for (PythonString prefix : prefixTuple) {
            if (toCheck.startsWith(prefix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean startsWith(PythonString prefix, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());
        return PythonBoolean.valueOf(value.substring(startIndex, endIndex).startsWith(prefix.value));
    }

    public PythonBoolean startsWith(PythonLikeTuple<PythonString> prefixTuple, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        String toCheck = value.substring(startIndex, endIndex);
        for (PythonString prefix : prefixTuple) {
            if (toCheck.startsWith(prefix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonString suffix) {
        return PythonBoolean.valueOf(value.endsWith(suffix.value));
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonString> suffixTuple) {
        for (PythonString suffix : suffixTuple) {
            if (value.endsWith(suffix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonString suffix, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        return PythonBoolean.valueOf(value.substring(startIndex).endsWith(suffix.value));
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonString> suffixTuple, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        String toCheck = value.substring(startIndex);
        for (PythonString suffix : suffixTuple) {
            if (toCheck.endsWith(suffix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonBoolean endsWith(PythonString suffix, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        return PythonBoolean.valueOf(value.substring(startIndex, endIndex).endsWith(suffix.value));
    }

    public PythonBoolean endsWith(PythonLikeTuple<PythonString> suffixTuple, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        String toCheck = value.substring(startIndex, endIndex);
        for (PythonString suffix : suffixTuple) {
            if (toCheck.endsWith(suffix.value)) {
                return PythonBoolean.TRUE;
            }
        }
        return PythonBoolean.FALSE;
    }

    public PythonString expandTabs() {
        return expandTabs(PythonInteger.valueOf(8));
    }

    public PythonString expandTabs(PythonInteger tabsize) {
        int tabsizeAsInt = tabsize.value.intValueExact();

        int column = 0;
        int length = value.length();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            if (character == '\n' || character == '\r') {
                builder.append(character);
                column = 0;
                continue;
            }

            if (character == '\t') {
                int remainder = tabsizeAsInt - (column % tabsizeAsInt);
                builder.append(" ".repeat(remainder));
                column += remainder;
                continue;
            }

            builder.append(character);
            column++;
        }

        return PythonString.valueOf(builder.toString());
    }

    public PythonInteger findSubstringIndex(PythonString substring) {
        return PythonInteger.valueOf(value.indexOf(substring.value));
    }

    public PythonInteger findSubstringIndex(PythonString substring, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int result = value.indexOf(substring.value, startIndex);

        return PythonInteger.valueOf(result);
    }

    public PythonInteger findSubstringIndex(PythonString substring, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        int result = value.substring(startIndex, endIndex).indexOf(substring.value);
        return PythonInteger.valueOf(result < 0 ? result : result + startIndex);
    }

    public PythonInteger rightFindSubstringIndex(PythonString substring) {
        return PythonInteger.valueOf(value.lastIndexOf(substring.value));
    }

    public PythonInteger rightFindSubstringIndex(PythonString substring, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int result = value.substring(startIndex).lastIndexOf(substring.value);

        return PythonInteger.valueOf(result < 0 ? result : result + startIndex);
    }

    public PythonInteger rightFindSubstringIndex(PythonString substring, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());
        int result = value.substring(startIndex, endIndex).lastIndexOf(substring.value);

        return PythonInteger.valueOf(result < 0 ? result : result + startIndex);
    }

    public PythonString format(List<PythonLikeObject> positionalArguments, Map<PythonString, PythonLikeObject> namedArguments) {
        return PythonString.valueOf(StringFormatter.format(value, positionalArguments, namedArguments));
    }

    public PythonString formatMap(PythonLikeDict dict) {
        return format(Collections.emptyList(), (Map) dict);
    }

    public PythonString formatSelf() {
        return this;
    }

    public PythonString formatSelf(PythonString spec) {
        if (spec.value.isEmpty()) {
            return this;
        }

        DefaultFormatSpec formatSpec = DefaultFormatSpec.fromStringSpec(spec);

        StringBuilder out = new StringBuilder();

        switch (formatSpec.conversionType.orElse(DefaultFormatSpec.ConversionType.STRING)) {
            case STRING:
                out.append(value);
                break;
            default:
                throw new ValueError("Invalid conversion type for str: " + formatSpec.conversionType);
        }

        StringFormatter.align(out, formatSpec, DefaultFormatSpec.AlignmentOption.LEFT_ALIGN);
        return PythonString.valueOf(out.toString());
    }

    public PythonString interpolate(PythonLikeObject object) {
        if (object instanceof PythonLikeTuple) {
            return interpolate((PythonLikeTuple) object);
        } else if (object instanceof PythonLikeDict) {
            return interpolate((PythonLikeDict) object);
        } else {
            return interpolate(PythonLikeTuple.fromItems(object));
        }
    }

    public PythonString interpolate(PythonLikeTuple tuple) {
        return PythonString.valueOf(StringFormatter.printfInterpolate(value, tuple, StringFormatter.PrintfStringType.STRING));
    }

    public PythonString interpolate(PythonLikeDict dict) {
        return PythonString.valueOf(StringFormatter.printfInterpolate(value, dict, StringFormatter.PrintfStringType.STRING));
    }

    public PythonInteger findSubstringIndexOrError(PythonString substring) {
        int result = value.indexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result);
    }

    public PythonInteger findSubstringIndexOrError(PythonString substring, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());

        int result = value.indexOf(substring.value, startIndex);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result);
    }

    public PythonInteger findSubstringIndexOrError(PythonString substring, PythonInteger start, PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        int result = value.substring(startIndex, endIndex).indexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result + startIndex);
    }

    public PythonInteger rightFindSubstringIndexOrError(PythonString substring) {
        int result = value.lastIndexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result);
    }

    public PythonInteger rightFindSubstringIndexOrError(PythonString substring, PythonInteger start) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());

        int result = value.substring(startIndex).lastIndexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result + startIndex);
    }

    public PythonInteger rightFindSubstringIndexOrError(PythonString substring, PythonInteger start,
            PythonInteger end) {
        int startIndex = PythonSlice.asValidStartIntIndexForLength(start, value.length());
        int endIndex = PythonSlice.asValidEndIntIndexForLength(end, value.length());

        int result = value.substring(startIndex, endIndex).lastIndexOf(substring.value);
        if (result == -1) {
            throw new ValueError("substring not found");
        }
        return PythonInteger.valueOf(result + startIndex);
    }

    private PythonBoolean allCharactersHaveProperty(IntPredicate predicate) {
        int length = value.length();
        if (length == 0) {
            return PythonBoolean.FALSE;
        }

        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            if (!predicate.test(character)) {
                return PythonBoolean.FALSE;
            }
        }

        return PythonBoolean.TRUE;
    }

    public PythonBoolean isAlphaNumeric() {
        return allCharactersHaveProperty(
                character -> Character.isLetter(character) || Character.getNumericValue(character) != -1);
    }

    public PythonBoolean isAlpha() {
        return allCharactersHaveProperty(Character::isLetter);
    }

    public PythonBoolean isAscii() {
        if (value.isEmpty()) {
            return PythonBoolean.TRUE;
        }
        return allCharactersHaveProperty(character -> character <= 127);
    }

    public PythonBoolean isDecimal() {
        return allCharactersHaveProperty(Character::isDigit);
    }

    private boolean isSuperscriptOrSubscript(int character) {
        String characterName = Character.getName(character);
        return characterName.contains("SUPERSCRIPT") || characterName.contains("SUBSCRIPT");
    }

    public PythonBoolean isDigit() {
        return allCharactersHaveProperty(character -> {
            if (Character.getType(character) == Character.DECIMAL_DIGIT_NUMBER) {
                return true;
            }
            return Character.isDigit(character) || (Character.getNumericValue(character) >= 0 &&
                    isSuperscriptOrSubscript(character));
        });
    }

    public PythonBoolean isIdentifier() {
        int length = value.length();
        if (length == 0) {
            return PythonBoolean.FALSE;
        }

        char firstChar = value.charAt(0);
        if (!isPythonIdentifierStart(firstChar)) {
            return PythonBoolean.FALSE;
        }

        for (int i = 1; i < length; i++) {
            char character = value.charAt(i);

            if (!isPythonIdentifierPart(character)) {
                return PythonBoolean.FALSE;
            }
        }

        return PythonBoolean.TRUE;
    }

    private static boolean isPythonIdentifierStart(char character) {
        if (Character.isLetter(character)) {
            return true;
        }
        if (Character.getType(character) == Character.LETTER_NUMBER) {
            return true;
        }

        switch (character) {
            case '_':
            case 0x1885:
            case 0x1886:
            case 0x2118:
            case 0x212E:
            case 0x309B:
            case 0x309C:
                return true;
            default:
                return false;
        }
    }

    private static boolean isPythonIdentifierPart(char character) {
        if (isPythonIdentifierStart(character)) {
            return true;
        }
        switch (Character.getType(character)) {
            case Character.NON_SPACING_MARK:
            case Character.COMBINING_SPACING_MARK:
            case Character.DECIMAL_DIGIT_NUMBER:
            case Character.CONNECTOR_PUNCTUATION:
                return true;
        }

        switch (character) {
            case 0x00B7:
            case 0x0387:
            case 0x19DA:
                return true;
            default:
                return character >= 0x1369 && character <= 0x1371;
        }
    }

    private static boolean hasCase(int character) {
        return Character.isUpperCase(character) ||
                Character.isLowerCase(character) ||
                Character.isTitleCase(character);
    }

    private boolean hasCaseCharacters() {
        return !allCharactersHaveProperty(character -> !hasCase(character)).getBooleanValue();
    }

    public PythonBoolean isLower() {
        if (hasCaseCharacters()) {
            return allCharactersHaveProperty(character -> !hasCase(character) || Character.isLowerCase(character));
        } else {
            return PythonBoolean.FALSE;
        }
    }

    public PythonBoolean isNumeric() {
        return allCharactersHaveProperty(character -> {
            switch (Character.getType(character)) {
                case Character.OTHER_NUMBER:
                case Character.DECIMAL_DIGIT_NUMBER:
                    return true;
                default:
                    return !Character.isLetter(character) && Character.getNumericValue(character) != -1;
            }
        });
    }

    private static boolean isCharacterPrintable(int character) {
        if (character == ' ') {
            return true;
        }
        switch (Character.getType(character)) {
            // Others
            case Character.PRIVATE_USE:
            case Character.FORMAT:
            case Character.CONTROL:
            case Character.UNASSIGNED:

                // Separators
            case Character.SPACE_SEPARATOR:
            case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR:
                return false;

            default:
                return true;
        }
    }

    public PythonBoolean isPrintable() {
        if (value.isEmpty()) {
            return PythonBoolean.TRUE;
        }

        return allCharactersHaveProperty(PythonString::isCharacterPrintable);
    }

    public PythonBoolean isSpace() {
        return PythonBoolean.valueOf(!value.isEmpty() && value.isBlank());
    }

    public PythonBoolean isUpper() {
        if (hasCaseCharacters()) {
            return allCharactersHaveProperty(character -> !hasCase(character) || Character.isUpperCase(character));
        } else {
            return PythonBoolean.FALSE;
        }
    }

    enum CharacterCase {
        UNCASED,
        LOWER,
        UPPER;

        public static CharacterCase getCase(int character) {
            if (Character.isLowerCase(character)) {
                return LOWER;
            } else if (Character.isUpperCase(character)) {
                return UPPER;
            } else {
                return UNCASED;
            }
        }

        public static int swapCase(int character) {
            if (Character.isLowerCase(character)) {
                return Character.toUpperCase(character);
            } else if (Character.isUpperCase(character)) {
                return Character.toLowerCase(character);
            }
            return character;
        }
    }

    public PythonBoolean isTitle() {
        int length = value.length();
        if (length == 0) {
            return PythonBoolean.FALSE;
        }

        CharacterCase previousType = CharacterCase.UNCASED;
        for (int i = 0; i < length; i++) {
            char character = value.charAt(i);

            CharacterCase characterCase = CharacterCase.getCase(character);
            if (characterCase == CharacterCase.UNCASED && Character.isLetter(character)) {
                return PythonBoolean.FALSE;
            }

            switch (previousType) {
                case UNCASED:
                    if (characterCase != CharacterCase.UPPER) {
                        return PythonBoolean.FALSE;
                    }
                    break;
                case UPPER:
                case LOWER:
                    if (characterCase == CharacterCase.UPPER) {
                        return PythonBoolean.FALSE;
                    }
                    break;
            }
            previousType = characterCase;
        }
        return PythonBoolean.TRUE;
    }

    public PythonString join(PythonLikeObject iterable) {
        PythonIterator iterator = (PythonIterator) UnaryDunderBuiltin.ITERATOR.invoke(iterable);
        int index = 0;
        StringBuilder out = new StringBuilder();

        while (iterator.hasNext()) {
            PythonLikeObject maybeString = iterator.nextPythonItem();
            if (!(maybeString instanceof PythonString)) {
                throw new TypeError("sequence item " + index + ": expected str instance, "
                        + maybeString.$getType().getTypeName() + " found");
            }
            PythonString string = (PythonString) maybeString;
            out.append(string.value);
            if (iterator.hasNext()) {
                out.append(value);
            }
            index++;
        }

        return PythonString.valueOf(out.toString());
    }

    public PythonString strip() {
        return PythonString.valueOf(value.strip());
    }

    public PythonString strip(PythonNone ignored) {
        return strip();
    }

    public PythonString strip(PythonString toStrip) {
        int length = value.length();

        int start = 0;
        int end = length - 1;

        for (; start < length; start++) {
            if (toStrip.value.indexOf(value.charAt(start)) == -1) {
                break;
            }
        }

        if (start == length) {
            return EMPTY;
        }

        for (; end >= start; end--) {
            if (toStrip.value.indexOf(value.charAt(end)) == -1) {
                break;
            }
        }

        return PythonString.valueOf(value.substring(start, end + 1));
    }

    public PythonString leftStrip() {
        return PythonString.valueOf(value.stripLeading());
    }

    public PythonString leftStrip(PythonNone ignored) {
        return leftStrip();
    }

    public PythonString leftStrip(PythonString toStrip) {
        int length = value.length();
        for (int i = 0; i < length; i++) {
            if (toStrip.value.indexOf(value.charAt(i)) == -1) {
                return PythonString.valueOf(value.substring(i));
            }
        }
        return EMPTY;
    }

    public PythonString rightStrip() {
        return PythonString.valueOf(value.stripTrailing());
    }

    public PythonString rightStrip(PythonNone ignored) {
        return rightStrip();
    }

    public PythonString rightStrip(PythonString toStrip) {
        int length = value.length();
        for (int i = length - 1; i >= 0; i--) {
            if (toStrip.value.indexOf(value.charAt(i)) == -1) {
                return PythonString.valueOf(value.substring(0, i + 1));
            }
        }
        return EMPTY;
    }

    public PythonLikeTuple partition(PythonString seperator) {
        int firstIndex = value.indexOf(seperator.value);
        if (firstIndex != -1) {
            return PythonLikeTuple.fromItems(
                    PythonString.valueOf(value.substring(0, firstIndex)),
                    seperator,
                    PythonString.valueOf(value.substring(firstIndex + seperator.value.length())));
        } else {
            return PythonLikeTuple.fromItems(
                    this,
                    EMPTY,
                    EMPTY);
        }
    }

    public PythonLikeTuple rightPartition(PythonString seperator) {
        int lastIndex = value.lastIndexOf(seperator.value);
        if (lastIndex != -1) {
            return PythonLikeTuple.fromItems(
                    PythonString.valueOf(value.substring(0, lastIndex)),
                    seperator,
                    PythonString.valueOf(value.substring(lastIndex + seperator.value.length())));
        } else {
            return PythonLikeTuple.fromItems(
                    EMPTY,
                    EMPTY,
                    this);
        }
    }

    public PythonString removePrefix(PythonString prefix) {
        if (value.startsWith(prefix.value)) {
            return new PythonString(value.substring(prefix.value.length()));
        }
        return this;
    }

    public PythonString removeSuffix(PythonString suffix) {
        if (value.endsWith(suffix.value)) {
            return new PythonString(value.substring(0, value.length() - suffix.value.length()));
        }
        return this;
    }

    public PythonString replaceAll(PythonString old, PythonString replacement) {
        return PythonString.valueOf(value.replaceAll(Pattern.quote(old.value), replacement.value));
    }

    public PythonString replaceUpToCount(PythonString old, PythonString replacement, PythonInteger count) {
        int countAsInt = count.value.intValueExact();
        if (countAsInt < 0) { // negative count act the same as replace all
            return replaceAll(old, replacement);
        }

        Matcher matcher = Pattern.compile(Pattern.quote(old.value)).matcher(value);
        StringBuilder out = new StringBuilder();
        int start = 0;
        while (countAsInt > 0) {
            if (matcher.find()) {
                out.append(value, start, matcher.start());
                out.append(replacement.value);
                start = matcher.end();
            } else {
                break;
            }
            countAsInt--;
        }
        out.append(value.substring(start));
        return PythonString.valueOf(out.toString());
    }

    public PythonLikeList<PythonString> split() {
        return Arrays.stream(value.stripLeading().split("\\s+"))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> split(PythonNone ignored) {
        return split();
    }

    public PythonLikeList<PythonString> split(PythonString seperator) {
        return Arrays.stream(value.split(Pattern.quote(seperator.value), -1))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> split(PythonString seperator, PythonInteger maxSplits) {
        int maxSplitsAsInt = maxSplits.value.intValueExact();
        if (maxSplitsAsInt == -1) {
            return split(seperator);
        }

        return Arrays.stream(value.split(Pattern.quote(seperator.value), maxSplitsAsInt + 1))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> split(PythonNone ignored, PythonInteger maxSplits) {
        int maxSplitsAsInt = maxSplits.value.intValueExact();
        if (maxSplitsAsInt == -1) {
            return split();
        }

        return Arrays.stream(value.stripLeading().split("\\s+", maxSplitsAsInt + 1))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> rightSplit() {
        return split();
    }

    public PythonLikeList<PythonString> rightSplit(PythonNone ignored) {
        return split();
    }

    public PythonLikeList<PythonString> rightSplit(PythonString seperator) {
        return split(seperator);
    }

    public PythonLikeList<PythonString> rightSplit(PythonString seperator, PythonInteger maxSplits) {
        int maxSplitsAsInt = maxSplits.value.intValueExact();
        if (maxSplitsAsInt == -1) {
            return split(seperator);
        }

        String reversedValue = new StringBuilder(value.stripTrailing()).reverse().toString();
        String reversedSeperator = new StringBuilder(seperator.value).reverse().toString();

        return Arrays.stream(reversedValue.split(Pattern.quote(reversedSeperator), maxSplitsAsInt + 1))
                .map(reversedPart -> PythonString.valueOf(new StringBuilder(reversedPart).reverse().toString()))
                .collect(Collectors.collectingAndThen(Collectors.toCollection(PythonLikeList::new), l -> {
                    Collections.reverse(l);
                    return l;
                }));
    }

    public PythonLikeList<PythonString> rightSplit(PythonNone ignored, PythonInteger maxSplits) {
        int maxSplitsAsInt = maxSplits.value.intValueExact();
        if (maxSplitsAsInt == -1) {
            return split();
        }

        String reversedValue = new StringBuilder(value.stripTrailing()).reverse().toString();

        return Arrays.stream(reversedValue.split("\\s+", maxSplitsAsInt + 1))
                .map(reversedPart -> PythonString.valueOf(new StringBuilder(reversedPart).reverse().toString()))
                .collect(Collectors.collectingAndThen(Collectors.toCollection(PythonLikeList::new), l -> {
                    Collections.reverse(l);
                    return l;
                }));
    }

    public PythonLikeList<PythonString> splitLines() {
        if (value.isEmpty()) {
            return new PythonLikeList<>();
        }
        return Arrays.stream(value.split("\\R"))
                .map(PythonString::valueOf)
                .collect(Collectors.toCollection(PythonLikeList::new));
    }

    public PythonLikeList<PythonString> splitLines(PythonBoolean keepEnds) {
        if (!keepEnds.getBooleanValue()) {
            return splitLines();
        }

        // Use lookahead so the newline is included in the result
        return Arrays.stream(value.split("(?<=\\R)"))
                .map(PythonString::valueOf)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(PythonLikeList::new), l -> {
                    int i;
                    for (i = 0; i < l.size() - 1; i++) {
                        // lookbehind cause it to split \r\n into two seperate
                        // lines; need to combine consecutive lines where the first ends with \r
                        // and the second starts with \n to get expected behavior
                        if (l.get(i).value.endsWith("\r") && l.get(i + 1).value.startsWith("\n")) {
                            l.set(i, PythonString.valueOf(l.get(i).value + l.remove(i + 1).value));
                            i--;
                        }
                    }

                    // Remove trailing empty string
                    // i = l.size() - 1
                    if (!l.isEmpty() && l.get(i).value.isEmpty()) {
                        l.remove(i);
                    }

                    return l;
                }));
    }

    public PythonString translate(PythonLikeObject object) {
        return PythonString.valueOf(value.codePoints()
                .flatMap(codePoint -> {
                    try {
                        PythonLikeObject translated =
                                BinaryDunderBuiltin.GET_ITEM.invoke(object, PythonInteger.valueOf(codePoint));
                        if (translated == PythonNone.INSTANCE) {
                            return IntStream.empty();
                        }

                        if (translated instanceof PythonInteger) {
                            return IntStream.of(((PythonInteger) translated).value.intValueExact());
                        }

                        if (translated instanceof PythonString) {
                            return ((PythonString) translated).value.codePoints();
                        }

                        throw new TypeError("character mapping must return integer, None or str");
                    } catch (LookupError e) {
                        return IntStream.of(codePoint);
                    }
                }).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
    }

    public PythonString zfill(PythonInteger width) {
        int widthAsInt = width.value.intValueExact();
        if (widthAsInt <= value.length()) {
            return this;
        }

        int leftPadding = widthAsInt - value.length();
        if (!value.isEmpty() && (value.charAt(0) == '+' || value.charAt(0) == '-')) {
            return PythonString.valueOf(value.charAt(0) + "0".repeat(leftPadding) + value.substring(1));
        } else {
            return PythonString.valueOf("0".repeat(leftPadding) + value);
        }
    }

    @Override
    public int compareTo(PythonString pythonString) {
        return value.compareTo(pythonString.value);
    }

    @Override
    public PythonString $method$__format__(PythonLikeObject specObject) {
        PythonString spec;
        if (specObject == PythonNone.INSTANCE) {
            return formatSelf();
        } else if (specObject instanceof PythonString) {
            spec = (PythonString) specObject;
        } else {
            throw new TypeError("__format__ argument 0 has incorrect type (expecting str or None)");
        }
        return formatSelf(spec);
    }

    public PythonString repr() {
        return PythonString.valueOf("'" + value.codePoints()
                .flatMap(character -> {
                    if (character == '\\') {
                        return IntStream.of('\\', '\\');
                    }
                    if (isCharacterPrintable(character)) {
                        return IntStream.of(character);
                    } else {
                        switch (character) {
                            case '\r':
                                return IntStream.of('\\', 'r');
                            case '\n':
                                return IntStream.of('\\', 'n');
                            case '\t':
                                return IntStream.of('\\', 't');
                            default: {
                                if (character < 0xFFFF) {
                                    return String.format("u%04x", character).codePoints();
                                } else {
                                    return String.format("U%08x", character).codePoints();
                                }

                            }
                        }
                    }
                })
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint, StringBuilder::append)
                .toString() + "'");
    }

    public PythonString asString() {
        return this;
    }

    @Override
    public PythonString $method$__str__() {
        return this;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof String) {
            return value.equals(o);
        } else if (o instanceof PythonString) {
            return ((PythonString) o).value.equals(value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
