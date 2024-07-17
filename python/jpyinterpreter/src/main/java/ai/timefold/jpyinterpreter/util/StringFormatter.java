package ai.timefold.jpyinterpreter.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.builtins.BinaryDunderBuiltin;
import ai.timefold.jpyinterpreter.builtins.GlobalBuiltins;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.PythonByteArray;
import ai.timefold.jpyinterpreter.types.PythonBytes;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.errors.lookup.KeyError;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class StringFormatter {
    final static String IDENTIFIER = "(?:(?:\\p{javaUnicodeIdentifierStart}|_)\\p{javaUnicodeIdentifierPart}*)";
    final static String ARG_NAME = "(?<argName>" + IDENTIFIER + "|\\d+)?";
    final static String ATTRIBUTE_NAME = IDENTIFIER;
    final static String ELEMENT_INDEX = "[^]]+";
    final static String ITEM_NAME = "(?:(?:\\." + ATTRIBUTE_NAME + ")|(?:\\[" + ELEMENT_INDEX + "\\]))";
    final static String FIELD_NAME = "(?<fieldName>" + ARG_NAME + "(" + ITEM_NAME + ")*)?";
    final static String CONVERSION = "(?:!(?<conversion>[rsa]))?";
    final static String FORMAT_SPEC = "(?::(?<formatSpec>[^{}]*))?";

    final static Pattern REPLACEMENT_FIELD_PATTERN = Pattern.compile("\\{" +
            FIELD_NAME +
            CONVERSION +
            FORMAT_SPEC +
            "}|(?<literal>\\{\\{|}})");

    final static Pattern INDEX_CHAIN_PART_PATTERN = Pattern.compile(ITEM_NAME);

    /**
     * Pattern that matches conversion specifiers for the "%" operator. See
     * <a href="https://docs.python.org/3/library/stdtypes.html#printf-style-string-formatting">
     * Python printf-style String Formatting documentation</a> for details.
     */
    private final static Pattern PRINTF_FORMAT_REGEX = Pattern.compile("%(?:(?<key>\\([^()]+\\))?" +
            "(?<flags>[#0\\-+ ]*)?" +
            "(?<minWidth>\\*|\\d+)?" +
            "(?<precision>\\.(?:\\*|\\d+))?" +
            "[hlL]?" + // ignored length modifier
            "(?<type>[diouxXeEfFgGcrsa%])|.*)");

    private enum PrintfConversionType {
        SIGNED_INTEGER_DECIMAL("d", "i", "u"),
        SIGNED_INTEGER_OCTAL("o"),
        SIGNED_HEXADECIMAL_LOWERCASE("x"),
        SIGNED_HEXADECIMAL_UPPERCASE("X"),
        FLOATING_POINT_EXPONENTIAL_LOWERCASE("e"),
        FLOATING_POINT_EXPONENTIAL_UPPERCASE("E"),
        FLOATING_POINT_DECIMAL("f", "F"),
        FLOATING_POINT_DECIMAL_OR_EXPONENTIAL_LOWERCASE("g"),
        FLOATING_POINT_DECIMAL_OR_EXPONENTIAL_UPPERCASE("G"),
        SINGLE_CHARACTER("c"),
        REPR_STRING("r"),
        STR_STRING("s"),
        ASCII_STRING("a"),
        LITERAL_PERCENT("%");

        final String[] matchedCharacters;

        PrintfConversionType(String... matchedCharacters) {
            this.matchedCharacters = matchedCharacters;
        }

        public static PrintfConversionType getConversionType(Matcher matcher) {
            String conversion = matcher.group("type");

            if (conversion == null) {
                throw new ValueError("Invalid specifier at position " + matcher.start() + " in string ");
            }

            for (PrintfConversionType conversionType : PrintfConversionType.values()) {
                for (String matchedCharacter : conversionType.matchedCharacters) {
                    if (matchedCharacter.equals(conversion)) {
                        return conversionType;
                    }
                }
            }
            throw new IllegalStateException("Conversion (" + conversion + ") does not match any defined conversions");
        }
    }

    public enum PrintfStringType {
        STRING,
        BYTES
    }

    public static String printfInterpolate(CharSequence value, List<PythonLikeObject> tuple, PrintfStringType stringType) {
        Matcher matcher = PRINTF_FORMAT_REGEX.matcher(value);

        StringBuilder out = new StringBuilder();
        int start = 0;
        int currentElement = 0;

        while (matcher.find()) {
            out.append(value, start, matcher.start());
            start = matcher.end();

            String key = matcher.group("key");
            if (key != null) {
                throw new TypeError("format requires a mapping");
            }

            String flags = matcher.group("flags");
            String minWidth = matcher.group("minWidth");
            String precisionString = matcher.group("precision");

            PrintfConversionType conversionType = PrintfConversionType.getConversionType(matcher);

            if (conversionType != PrintfConversionType.LITERAL_PERCENT) {
                if (tuple.size() <= currentElement) {
                    throw new TypeError("not enough arguments for format string");
                }

                PythonLikeObject toConvert = tuple.get(currentElement);

                currentElement++;

                if ("*".equals(minWidth)) {
                    if (tuple.size() <= currentElement) {
                        throw new TypeError("not enough arguments for format string");
                    }
                    minWidth = ((PythonString) UnaryDunderBuiltin.STR.invoke(tuple.get(currentElement))).value;
                    currentElement++;
                }

                if ("*".equals(precisionString)) {
                    if (tuple.size() <= currentElement) {
                        throw new TypeError("not enough arguments for format string");
                    }
                    precisionString = ((PythonString) UnaryDunderBuiltin.STR.invoke(tuple.get(currentElement))).value;
                    currentElement++;
                }

                Optional<Integer> maybePrecision, maybeWidth;
                if (precisionString != null) {
                    maybePrecision = Optional.of(Integer.parseInt(precisionString.substring(1)));
                } else {
                    maybePrecision = Optional.empty();
                }

                if (minWidth != null) {
                    maybeWidth = Optional.of(Integer.parseInt(minWidth));
                } else {
                    maybeWidth = Optional.empty();
                }
                out.append(performInterpolateConversion(flags, maybeWidth, maybePrecision, conversionType, toConvert,
                        stringType));
            } else {
                out.append("%");
            }
        }

        out.append(value.subSequence(start, value.length()));

        return out.toString();
    }

    public static String printfInterpolate(CharSequence value, PythonLikeDict dict, PrintfStringType stringType) {
        Matcher matcher = PRINTF_FORMAT_REGEX.matcher(value);

        StringBuilder out = new StringBuilder();
        int start = 0;
        while (matcher.find()) {
            out.append(value, start, matcher.start());
            start = matcher.end();

            PrintfConversionType conversionType = PrintfConversionType.getConversionType(matcher);

            if (conversionType != PrintfConversionType.LITERAL_PERCENT) {
                String key = matcher.group("key");
                if (key == null) {
                    throw new ValueError(
                            "When a dict is used for the interpolation operator, all conversions must have parenthesised keys");
                }
                key = key.substring(1, key.length() - 1);

                String flags = matcher.group("flags");
                String minWidth = matcher.group("minWidth");
                String precisionString = matcher.group("precision");

                if ("*".equals(minWidth)) {
                    throw new ValueError(
                            "* cannot be used for minimum field width when a dict is used for the interpolation operator");
                }

                if ("*".equals(precisionString)) {
                    throw new ValueError("* cannot be used for precision when a dict is used for the interpolation operator");
                }

                PythonLikeObject toConvert;
                if (stringType == PrintfStringType.STRING) {
                    toConvert = dict.getItemOrError(PythonString.valueOf(key));
                } else {
                    toConvert = dict.getItemOrError(PythonString.valueOf(key).asAsciiBytes());
                }

                Optional<Integer> maybePrecision, maybeWidth;
                if (precisionString != null) {
                    maybePrecision = Optional.of(Integer.parseInt(precisionString.substring(1)));
                } else {
                    maybePrecision = Optional.empty();
                }

                if (minWidth != null) {
                    maybeWidth = Optional.of(Integer.parseInt(minWidth));
                } else {
                    maybeWidth = Optional.empty();
                }

                out.append(performInterpolateConversion(flags, maybeWidth, maybePrecision, conversionType, toConvert,
                        stringType));
            } else {
                out.append("%");
            }
        }

        out.append(value.subSequence(start, value.length()));
        return out.toString();
    }

    private static BigDecimal getBigDecimalWithPrecision(BigDecimal number, Optional<Integer> precision) {
        int currentScale = number.scale();
        int currentPrecision = number.precision();
        int precisionDelta = precision.orElse(6) - currentPrecision;
        return number.setScale(currentScale + precisionDelta, RoundingMode.HALF_EVEN);
    }

    private static String getUppercaseEngineeringString(BigDecimal number, Optional<Integer> precision) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        printStream.printf("%1." + (precision.orElse(6) - 1) + "E", number);
        return out.toString();
    }

    private static String performInterpolateConversion(String flags, Optional<Integer> maybeWidth,
            Optional<Integer> maybePrecision,
            PrintfConversionType conversionType,
            PythonLikeObject toConvert,
            PrintfStringType stringType) {
        boolean useAlternateForm = flags.contains("#");
        boolean isZeroPadded = flags.contains("0");
        boolean isLeftAdjusted = flags.contains("-");
        if (isLeftAdjusted) {
            isZeroPadded = false;
        }

        boolean putSpaceBeforePositiveNumber = flags.contains(" ");
        boolean putSignBeforeConversion = flags.contains("+");
        if (putSignBeforeConversion) {
            putSpaceBeforePositiveNumber = false;
        }

        String result;
        switch (conversionType) {
            case SIGNED_INTEGER_DECIMAL: {
                if (toConvert instanceof PythonFloat) {
                    toConvert = ((PythonFloat) toConvert).asInteger();
                }
                if (!(toConvert instanceof PythonInteger)) {
                    throw new TypeError("%d format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                result = ((PythonInteger) toConvert).value.toString(10);
                break;
            }
            case SIGNED_INTEGER_OCTAL: {
                if (toConvert instanceof PythonFloat) {
                    toConvert = ((PythonFloat) toConvert).asInteger();
                }
                if (!(toConvert instanceof PythonInteger)) {
                    throw new TypeError("%o format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                result = ((PythonInteger) toConvert).value.toString(8);
                if (useAlternateForm) {
                    result = (result.startsWith("-")) ? "-0o" + result.substring(1) : "0o" + result;
                }
                break;
            }
            case SIGNED_HEXADECIMAL_LOWERCASE: {
                if (toConvert instanceof PythonFloat) {
                    toConvert = ((PythonFloat) toConvert).asInteger();
                }
                if (!(toConvert instanceof PythonInteger)) {
                    throw new TypeError("%x format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                result = ((PythonInteger) toConvert).value.toString(16);
                if (useAlternateForm) {
                    result = (result.startsWith("-")) ? "-0x" + result.substring(1) : "0x" + result;
                }
                break;
            }
            case SIGNED_HEXADECIMAL_UPPERCASE: {
                if (toConvert instanceof PythonFloat) {
                    toConvert = ((PythonFloat) toConvert).asInteger();
                }
                if (!(toConvert instanceof PythonInteger)) {
                    throw new TypeError("%X format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                result = ((PythonInteger) toConvert).value.toString(16).toUpperCase();
                if (useAlternateForm) {
                    result = (result.startsWith("-")) ? "-0X" + result.substring(1) : "0X" + result;
                }
                break;
            }
            case FLOATING_POINT_EXPONENTIAL_LOWERCASE: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%e format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                result = getUppercaseEngineeringString(value, maybePrecision.map(precision -> precision + 1)
                        .or(() -> Optional.of(7))).toLowerCase();
                if (useAlternateForm && !result.contains(".")) {
                    result = result + ".0";
                }
                break;
            }
            case FLOATING_POINT_EXPONENTIAL_UPPERCASE: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%E format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                result = getUppercaseEngineeringString(value, maybePrecision.map(precision -> precision + 1)
                        .or(() -> Optional.of(7)));
                if (useAlternateForm && !result.contains(".")) {
                    result = result + ".0";
                }
                break;
            }
            case FLOATING_POINT_DECIMAL: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%f format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                BigDecimal valueWithPrecision = value.setScale(maybePrecision.orElse(6), RoundingMode.HALF_EVEN);
                result = valueWithPrecision.toPlainString();
                if (useAlternateForm && !result.contains(".")) {
                    result = result + ".0";
                }
                break;
            }
            case FLOATING_POINT_DECIMAL_OR_EXPONENTIAL_LOWERCASE: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%g format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                BigDecimal valueWithPrecision;

                if (value.scale() > 4 || value.precision() >= maybePrecision.orElse(6)) {
                    valueWithPrecision = getBigDecimalWithPrecision(value, maybePrecision);
                    result = getUppercaseEngineeringString(valueWithPrecision, maybePrecision).toLowerCase();
                } else {
                    valueWithPrecision = value.setScale(maybePrecision.orElse(6), RoundingMode.HALF_EVEN);
                    result = valueWithPrecision.toPlainString();
                }

                if (result.length() >= 3 && result.charAt(result.length() - 3) == 'e') {
                    result = result.substring(0, result.length() - 1) + "0" + result.charAt(result.length() - 1);
                }
                break;
            }
            case FLOATING_POINT_DECIMAL_OR_EXPONENTIAL_UPPERCASE: {
                if (toConvert instanceof PythonInteger) {
                    toConvert = ((PythonInteger) toConvert).asFloat();
                }
                if (!(toConvert instanceof PythonFloat)) {
                    throw new TypeError("%G format: a real number is required, not " + toConvert.$getType().getTypeName());
                }
                BigDecimal value = BigDecimal.valueOf(((PythonFloat) toConvert).value);
                BigDecimal valueWithPrecision;

                if (value.scale() > 4 || value.precision() >= maybePrecision.orElse(6)) {
                    valueWithPrecision = getBigDecimalWithPrecision(value, maybePrecision);
                    result = getUppercaseEngineeringString(valueWithPrecision, maybePrecision);
                } else {
                    valueWithPrecision = value.setScale(maybePrecision.orElse(6), RoundingMode.HALF_EVEN);
                    result = valueWithPrecision.toPlainString();
                }
                break;
            }
            case SINGLE_CHARACTER: {
                if (stringType == PrintfStringType.STRING) {
                    if (toConvert instanceof PythonString) {
                        PythonString convertedCharacter = (PythonString) toConvert;
                        if (convertedCharacter.value.length() != 1) {
                            throw new ValueError("c specifier can only take an integer or single character string");
                        }
                        result = convertedCharacter.value;
                    } else {
                        result = Character.toString(((PythonInteger) toConvert).value.intValueExact());
                    }
                } else {
                    if (toConvert instanceof PythonBytes) {
                        PythonBytes convertedCharacter = (PythonBytes) toConvert;
                        if (convertedCharacter.value.length != 1) {
                            throw new ValueError("c specifier can only take an integer or single character string");
                        }
                        result = convertedCharacter.asCharSequence().toString();
                    } else if (toConvert instanceof PythonByteArray) {
                        PythonByteArray convertedCharacter = (PythonByteArray) toConvert;
                        if (convertedCharacter.valueBuffer.limit() != 1) {
                            throw new ValueError("c specifier can only take an integer or single character string");
                        }
                        result = convertedCharacter.asCharSequence().toString();
                    } else {
                        result = Character.toString(((PythonInteger) toConvert).value.intValueExact());
                    }
                }
                break;
            }
            case REPR_STRING: {
                result = ((PythonString) UnaryDunderBuiltin.REPRESENTATION.invoke(toConvert)).value;
                break;
            }
            case STR_STRING: {
                if (stringType == PrintfStringType.STRING) {
                    result = ((PythonString) UnaryDunderBuiltin.STR.invoke(toConvert)).value;
                } else {
                    if (toConvert instanceof PythonBytes) {
                        result = ((PythonBytes) toConvert).asCharSequence().toString();
                    } else if (toConvert instanceof PythonByteArray) {
                        result = ((PythonByteArray) toConvert).asCharSequence().toString();
                    } else {
                        result = ((PythonString) UnaryDunderBuiltin.STR.invoke(toConvert)).value;
                    }
                }
                break;
            }
            case ASCII_STRING: {
                result = GlobalBuiltins.ascii(List.of(toConvert), Map.of(), null).value;
                break;
            }
            case LITERAL_PERCENT: {
                result = "%";
                break;
            }
            default:
                throw new IllegalStateException("Unhandled case: " + conversionType);
        }

        if (putSignBeforeConversion && !(result.startsWith("+") || result.startsWith("-"))) {
            result = "+" + result;
        }

        if (putSpaceBeforePositiveNumber && !(result.startsWith("-"))) {
            result = " " + result;
        }

        if (maybeWidth.isPresent() && maybeWidth.get() > result.length()) {
            int padding = maybeWidth.get() - result.length();
            if (isZeroPadded) {
                if (result.startsWith("+") || result.startsWith("-")) {
                    result = result.charAt(0) + "0".repeat(padding) + result.substring(1);
                } else {
                    result = "0".repeat(padding) + result;
                }
            } else if (isLeftAdjusted) {
                result = result + " ".repeat(padding);
            }
        }

        return result;
    }

    public static String format(String text, List<PythonLikeObject> positionalArguments,
            Map<? extends PythonLikeObject, PythonLikeObject> namedArguments) {
        Matcher matcher = REPLACEMENT_FIELD_PATTERN.matcher(text);
        StringBuilder out = new StringBuilder();
        int start = 0;
        int implicitField = 0;

        while (matcher.find()) {
            out.append(text, start, matcher.start());
            start = matcher.end();

            String literal = matcher.group("literal");
            if (literal != null) {
                switch (literal) {
                    case "{{":
                        out.append("{");
                        continue;
                    case "}}":
                        out.append("}");
                        continue;
                    default:
                        throw new IllegalStateException("Unhandled literal: " + literal);
                }
            }

            String argName = matcher.group("argName");

            PythonLikeObject toConvert;

            if (positionalArguments != null) {
                if (argName == null) {
                    if (implicitField >= positionalArguments.size()) {
                        throw new ValueError(
                                "(" + implicitField + ") is larger than sequence length (" + positionalArguments.size() + ")");
                    }
                    toConvert = positionalArguments.get(implicitField);
                    implicitField++;
                } else {
                    try {
                        int argumentIndex = Integer.parseInt(argName);
                        if (argumentIndex >= positionalArguments.size()) {
                            throw new ValueError("(" + implicitField + ") is larger than sequence length ("
                                    + positionalArguments.size() + ")");
                        }
                        toConvert = positionalArguments.get(argumentIndex);
                    } catch (NumberFormatException e) {
                        if (namedArguments == null) {
                            throw new ValueError("(" + argName + ") cannot be used to index a sequence");
                        } else {
                            toConvert = namedArguments.get(PythonString.valueOf(argName));
                        }
                    }
                }
            } else {
                toConvert = namedArguments.get(PythonString.valueOf(argName));
            }

            if (toConvert == null) {
                throw new KeyError(argName);
            }

            toConvert = getFinalObjectInChain(toConvert, matcher.group("fieldName"));

            String conversion = matcher.group("conversion");
            if (conversion != null) {
                switch (conversion) {
                    case "s":
                        toConvert = UnaryDunderBuiltin.STR.invoke(toConvert);
                        break;
                    case "r":
                        toConvert = UnaryDunderBuiltin.REPRESENTATION.invoke(toConvert);
                        break;
                    case "a":
                        toConvert = GlobalBuiltins.ascii(List.of(toConvert), Map.of(), null);
                        break;
                }
            }

            String formatSpec = Objects.requireNonNullElse(matcher.group("formatSpec"), "");
            out.append(BinaryDunderBuiltin.FORMAT.invoke(toConvert, PythonString.valueOf(formatSpec)));
        }
        out.append(text.substring(start));
        return out.toString();
    }

    private static PythonLikeObject getFinalObjectInChain(PythonLikeObject chainStart, String chain) {
        if (chain == null) {
            return chainStart;
        }

        PythonLikeObject current = chainStart;
        Matcher matcher = INDEX_CHAIN_PART_PATTERN.matcher(chain);

        while (matcher.find()) {
            String result = matcher.group();
            if (result.startsWith(".")) {
                String attributeName = result.substring(1);
                current = BinaryDunderBuiltin.GET_ATTRIBUTE.invoke(current, PythonString.valueOf(attributeName));
            } else {
                String index = result.substring(1, result.length() - 1);
                try {
                    int intIndex = Integer.parseInt(index);
                    current = BinaryDunderBuiltin.GET_ITEM.invoke(current, PythonInteger.valueOf(intIndex));
                } catch (NumberFormatException e) {
                    current = BinaryDunderBuiltin.GET_ITEM.invoke(current, PythonString.valueOf(index));
                }
            }
        }
        return current;
    }

    public static void addGroupings(StringBuilder out, DefaultFormatSpec formatSpec, int groupSize) {
        if (formatSpec.groupingOption.isEmpty()) {
            return;
        }

        if (groupSize <= 0) {
            throw new ValueError(
                    "Invalid format spec: grouping option now allowed for conversion type " + formatSpec.conversionType);
        }

        int decimalSeperator = out.indexOf(".");
        char seperator;
        switch (formatSpec.groupingOption.get()) {
            case COMMA:
                seperator = ',';
                break;
            case UNDERSCORE:
                seperator = '_';
                break;
            default:
                throw new IllegalStateException("Unhandled case: " + formatSpec.groupingOption.get());
        }

        int index;
        if (decimalSeperator != -1) {
            index = decimalSeperator - 1;
        } else {
            index = out.length() - 1;
        }

        int groupIndex = 0;
        while (index >= 0 && out.charAt(index) != '-') {
            groupIndex++;
            if (groupIndex == groupSize) {
                out.insert(index, seperator);
                groupIndex = 0;
            }
            index--;
        }
    }

    public static void align(StringBuilder out, DefaultFormatSpec formatSpec,
            DefaultFormatSpec.AlignmentOption defaultAlignment) {
        if (formatSpec.width.isPresent()) {
            switch (formatSpec.alignment.orElse(defaultAlignment)) {
                case LEFT_ALIGN:
                    leftAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case RIGHT_ALIGN:
                    rightAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case RESPECT_SIGN_RIGHT_ALIGN:
                    respectSignRightAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case CENTER_ALIGN:
                    center(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
            }
        }
    }

    public static void alignWithPrefixRespectingSign(StringBuilder out, String prefix, DefaultFormatSpec formatSpec,
            DefaultFormatSpec.AlignmentOption defaultAlignment) {
        int insertPosition = (out.charAt(0) == '+' || out.charAt(0) == '-' || out.charAt(0) == ' ') ? 1 : 0;
        if (formatSpec.width.isPresent()) {
            switch (formatSpec.alignment.orElse(defaultAlignment)) {
                case LEFT_ALIGN:
                    out.insert(insertPosition, prefix);
                    leftAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case RIGHT_ALIGN:
                    out.insert(insertPosition, prefix);
                    rightAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
                case RESPECT_SIGN_RIGHT_ALIGN:
                    respectSignRightAlign(out, formatSpec.fillCharacter, formatSpec.width.get());
                    out.insert(insertPosition, prefix);
                    break;
                case CENTER_ALIGN:
                    out.insert(insertPosition, prefix);
                    center(out, formatSpec.fillCharacter, formatSpec.width.get());
                    break;
            }
        } else {
            out.insert(insertPosition, prefix);
        }
    }

    public static void leftAlign(StringBuilder builder, String fillCharAsString, int width) {
        if (width <= builder.length()) {
            return;
        }

        int rightPadding = width - builder.length();
        builder.append(fillCharAsString.repeat(rightPadding));
    }

    public static void rightAlign(StringBuilder builder, String fillCharAsString, int width) {
        if (width <= builder.length()) {
            return;
        }

        int leftPadding = width - builder.length();
        builder.insert(0, fillCharAsString.repeat(leftPadding));
    }

    public static void respectSignRightAlign(StringBuilder builder, String fillCharAsString, int width) {
        if (width <= builder.length()) {
            return;
        }

        int leftPadding = width - builder.length();
        if (builder.length() >= 1 && (builder.charAt(0) == '+' || builder.charAt(0) == '-' || builder.charAt(0) == ' ')) {
            builder.insert(1, fillCharAsString.repeat(leftPadding));
        } else {
            builder.insert(0, fillCharAsString.repeat(leftPadding));
        }
    }

    public static void center(StringBuilder builder, String fillCharAsString, int width) {
        if (width <= builder.length()) {
            return;
        }
        int extraWidth = width - builder.length();
        int rightPadding = extraWidth / 2;
        // left padding get extra character if extraWidth is odd
        int leftPadding = rightPadding + (extraWidth & 1); // x & 1 == x % 2

        builder.insert(0, fillCharAsString.repeat(leftPadding))
                .append(fillCharAsString.repeat(rightPadding));
    }
}
