package ai.timefold.jpyinterpreter.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.ValueError;

public class DefaultFormatSpec {
    final static String FILL = "(?<fill>.)?";
    final static String ALIGN = "(?:" + FILL + "(?<align>[<>=^]))?";
    final static String SIGN = "(?<sign>[+\\- ])?";
    final static String ALTERNATE_FORM = "(?<alternateForm>#)?";
    final static String SIGN_AWARE_ZERO_FILL = "(?<signAwareZeroFill>0)?";
    final static String WIDTH = "(?<width>\\d+)?";
    final static String GROUPING_OPTION = "(?<groupingOption>[_,])?";
    final static String PRECISION = "(?:\\.(?<precision>\\d+))?";
    final static String TYPE = "(?<type>[bcdeEfFgGnosxX%])?";
    final static String DEFAULT_FORMAT_SPEC = ALIGN +
            SIGN +
            ALTERNATE_FORM +
            SIGN_AWARE_ZERO_FILL +
            WIDTH +
            GROUPING_OPTION +
            PRECISION +
            TYPE;
    final static Pattern DEFAULT_FORMAT_SPEC_PATTERN = Pattern.compile(DEFAULT_FORMAT_SPEC);

    /**
     * The character to use for padding
     */
    public final String fillCharacter;

    /**
     * If true, modify the displayed output for some {@link ConversionType}
     */
    public final boolean useAlternateForm;

    /**
     * How padding should be applied to fill width
     */
    public final Optional<AlignmentOption> alignment;

    public final Optional<SignOption> signOption;

    /**
     * The minimum space the output should satisfy
     */
    public final Optional<Integer> width;

    /**
     * What to use for the thousands' seperator
     */
    public final Optional<GroupingOption> groupingOption;

    /**
     * How many significant digits for floating point numbers. For strings, the
     * maximum space the output should satisfy. Not allowed for int types.
     */
    public final Optional<Integer> precision;

    /**
     * How the value should be displayed
     */
    public final Optional<ConversionType> conversionType;

    private DefaultFormatSpec(String fillCharacter, boolean useAlternateForm,
            Optional<AlignmentOption> alignment, Optional<SignOption> signOption,
            Optional<Integer> width, Optional<GroupingOption> groupingOption, Optional<Integer> precision,
            Optional<ConversionType> conversionType) {
        this.fillCharacter = fillCharacter;
        this.useAlternateForm = useAlternateForm;
        this.alignment = alignment;
        this.signOption = signOption;
        this.width = width;
        this.groupingOption = groupingOption;
        this.precision = precision;
        this.conversionType = conversionType;
    }

    public static DefaultFormatSpec fromSpec(PythonString formatSpec) {
        Matcher matcher = DEFAULT_FORMAT_SPEC_PATTERN.matcher(formatSpec.value);

        if (!matcher.matches()) {
            throw new ValueError("Invalid format spec: " + formatSpec.value);
        }

        Optional<String> signAwareZeroFill = Optional.ofNullable(matcher.group("signAwareZeroFill"));

        return new DefaultFormatSpec(
                Optional.ofNullable(matcher.group("fill")).or(() -> signAwareZeroFill).orElse(" "),
                Optional.ofNullable(matcher.group("alternateForm")).isPresent(),
                Optional.ofNullable(matcher.group("align")).map(AlignmentOption::fromString)
                        .or(() -> signAwareZeroFill.map(ignored -> AlignmentOption.RESPECT_SIGN_RIGHT_ALIGN)),
                Optional.ofNullable(matcher.group("sign")).map(SignOption::fromString),
                Optional.ofNullable(matcher.group("width")).map(Integer::parseInt),
                Optional.ofNullable(matcher.group("groupingOption")).map(GroupingOption::fromString),
                Optional.ofNullable(matcher.group("precision")).map(Integer::parseInt),
                Optional.ofNullable(matcher.group("type")).map(ConversionType::fromString));
    }

    /**
     * For use by {@link PythonString}, where since Python 3.10, 0 before width do not affect default alignment
     * of strings.
     */
    public static DefaultFormatSpec fromStringSpec(PythonString formatSpec) {
        Matcher matcher = DEFAULT_FORMAT_SPEC_PATTERN.matcher(formatSpec.value);

        if (!matcher.matches()) {
            throw new ValueError("Invalid format spec: " + formatSpec.value);
        }

        Optional<String> signAwareZeroFill = Optional.ofNullable(matcher.group("signAwareZeroFill"));

        return new DefaultFormatSpec(
                Optional.ofNullable(matcher.group("fill")).or(() -> signAwareZeroFill).orElse(" "),
                Optional.ofNullable(matcher.group("alternateForm")).isPresent(),
                Optional.ofNullable(matcher.group("align")).map(AlignmentOption::fromString),
                Optional.ofNullable(matcher.group("sign")).map(SignOption::fromString),
                Optional.ofNullable(matcher.group("width")).map(Integer::parseInt),
                Optional.ofNullable(matcher.group("groupingOption")).map(GroupingOption::fromString),
                Optional.ofNullable(matcher.group("precision")).map(Integer::parseInt),
                Optional.ofNullable(matcher.group("type")).map(ConversionType::fromString));
    }

    public int getPrecisionOrDefault() {
        return precision.orElse(6);
    }

    public enum AlignmentOption {
        /**
         * Forces the field to be left-aligned within the available space (this is the default for most objects).
         */
        LEFT_ALIGN("<"),
        /**
         * Forces the field to be right-aligned within the available space (this is the default for numbers).
         */
        RIGHT_ALIGN(">"),
        /**
         * Forces the padding to be placed after the sign (if any) but before the digits.
         * This is used for printing fields in the form ‘+000000120’.
         * This alignment option is only valid for numeric types.
         * It becomes the default for numbers when ‘0’ immediately precedes the field width.
         */
        RESPECT_SIGN_RIGHT_ALIGN("="),

        /**
         * Forces the field to be centered within the available space.
         */
        CENTER_ALIGN("^");

        final String matchCharacter;

        AlignmentOption(String matchCharacter) {
            this.matchCharacter = matchCharacter;
        }

        public static AlignmentOption fromString(String text) {
            for (AlignmentOption alignmentOption : AlignmentOption.values()) {
                if (alignmentOption.matchCharacter.equals(text)) {
                    return alignmentOption;
                }
            }
            throw new IllegalArgumentException("\"" + text + "\" does not match any alignment option");
        }
    }

    public enum SignOption {
        ALWAYS_SIGN("+"),
        ONLY_NEGATIVE_NUMBERS("-"),
        SPACE_FOR_POSITIVE_NUMBERS(" ");

        final String matchCharacter;

        SignOption(String matchCharacter) {
            this.matchCharacter = matchCharacter;
        }

        public static SignOption fromString(String text) {
            for (SignOption signOption : SignOption.values()) {
                if (signOption.matchCharacter.equals(text)) {
                    return signOption;
                }
            }
            throw new IllegalArgumentException("\"" + text + "\" does not match any sign option");
        }
    }

    public enum GroupingOption {
        /**
         * Signals the use of a comma for the thousands' separator.
         */
        COMMA(","),

        /**
         * Signals the use of an underscore for the thousands' separator.
         */
        UNDERSCORE("_");

        final String matchCharacter;

        GroupingOption(String matchCharacter) {
            this.matchCharacter = matchCharacter;
        }

        public static GroupingOption fromString(String text) {
            for (GroupingOption groupingOption : GroupingOption.values()) {
                if (groupingOption.matchCharacter.equals(text)) {
                    return groupingOption;
                }
            }
            throw new IllegalArgumentException("\"" + text + "\" does not match any grouping option");
        }
    }

    public enum ConversionType {
        // String
        /**
         * (string) String format. This is the default type for strings and may be omitted.
         */
        STRING("s"),

        // Integer
        /**
         * (int) Binary format. Outputs the number in base 2.
         */
        BINARY("b"),

        /**
         * (int) Character. Converts the integer to the corresponding unicode character before printing.
         */
        CHARACTER("c"),

        /**
         * (int) Decimal Integer. Outputs the number in base 10.
         */
        DECIMAL("d"),

        /**
         * (int) Octal format. Outputs the number in base 8.
         */
        OCTAL("o"),

        /**
         * (int) Hex format. Outputs the number in base 16, using lower-case letters for the digits above 9.
         */
        LOWERCASE_HEX("x"),

        /**
         * (int) Hex format. Outputs the number in base 16, using upper-case letters for the digits above 9.
         * In case '#' is specified, the prefix '0x' will be upper-cased to '0X' as well.
         */
        UPPERCASE_HEX("X"),

        // Float
        /**
         * (float) Scientific notation. For a given precision p,
         * formats the number in scientific notation with the letter ‘e’ separating the coefficient from the exponent.
         * The coefficient has one digit before and p digits after the decimal point, for a total of p + 1 significant digits.
         * With no precision given, uses a precision of 6 digits after the decimal point for float,
         * and shows all coefficient digits for Decimal. If no digits follow the decimal point,
         * the decimal point is also removed unless the # option is used.
         */
        LOWERCASE_SCIENTIFIC_NOTATION("e"),

        /**
         * (float) Same as {@link #LOWERCASE_SCIENTIFIC_NOTATION} except it uses an upper case ‘E’ as the separator character.
         */
        UPPERCASE_SCIENTIFIC_NOTATION("E"),

        /**
         * (float) Fixed-point notation. For a given precision p, formats the number as a decimal number
         * with exactly p digits following the decimal point. With no precision given,
         * uses a precision of 6 digits after the decimal point for float, and uses a precision large enough to show
         * all coefficient digits for Decimal. If no digits follow the decimal point, the decimal point is also removed
         * unless the # option is used.
         */
        LOWERCASE_FIXED_POINT("f"),

        /**
         * (float) Same as {@link #LOWERCASE_FIXED_POINT} but converts "nan" to "NAN" and "inf" to "INF"
         */
        UPPERCASE_FIXED_POINT("F"),

        /**
         * (float) General format. For a given precision p >= 1, this rounds the number to p significant digits and then
         * formats the result in either fixed-point format or in scientific notation, depending on its magnitude.
         * A precision of 0 is treated as equivalent to a precision of 1.
         *
         * The precise rules are as follows:
         * suppose that the result formatted with presentation type {@link #LOWERCASE_SCIENTIFIC_NOTATION}
         * and precision p-1 would have exponent exp.
         * Then, if m &le; exp &lt; p, where m is -4 for floats and -6 for Decimals,
         * the number is formatted with presentation type 'f'and precision p-1-exp.
         * Otherwise, the number is formatted with presentation type 'e' and precision p-1.
         * In both cases insignificant trailing zeros are removed from the significand,
         * and the decimal point is also removed if there are no remaining digits following it,
         * unless the '#' option is used.
         *
         * With no precision given, uses a precision of 6 significant digits for float.
         * For Decimal, the coefficient of the result is formed from the coefficient digits of the value;
         * scientific notation is used for values smaller than 1e-6 in absolute value and values where
         * the place value of the least significant digit is larger than 1, and fixed-point notation is used otherwise.
         *
         * Positive and negative infinity, positive and negative zero, and nans, are formatted as
         * inf, -inf, 0, -0 and nan respectively, regardless of the precision.
         */
        LOWERCASE_GENERAL("g"),

        /**
         * (float) Same as {@link #LOWERCASE_GENERAL}, except switches to {#UPPERCASE_SCIENTIFIC_NOTATION} if the number gets
         * too large.
         * The representations of infinity and NaN are uppercased, too.
         */
        UPPERCASE_GENERAL("G"),

        /**
         * (int, float) Locale sensitive number output. For integers, same as {@link #DECIMAL} but it will use the
         * current locale's number seperator. For floats, same as {@link #LOWERCASE_GENERAL},
         * except that when fixed-point notation is used to format the result,
         * it always includes at least one digit past the decimal point.
         * The precision used is as large as needed to represent the given value faithfully.
         */
        LOCALE_SENSITIVE("n"),

        /**
         * (float) Percentage. Multiplies the number by 100 and displays in {@link #LOWERCASE_FIXED_POINT} format,
         * followed by a percent sign.
         */
        PERCENTAGE("%");

        final String matchCharacter;

        ConversionType(String matchCharacter) {
            this.matchCharacter = matchCharacter;
        }

        public static ConversionType fromString(String text) {
            for (ConversionType conversionType : ConversionType.values()) {
                if (conversionType.matchCharacter.equals(text)) {
                    return conversionType;
                }
            }
            throw new IllegalArgumentException("\"" + text + "\" does not match any conversion type");
        }
    }
}
