package ai.timefold.jpyinterpreter.types.datetime;

import static ai.timefold.jpyinterpreter.types.datetime.PythonDateTime.DATE_TIME_TYPE;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeComparable;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.numeric.PythonNumber;
import ai.timefold.jpyinterpreter.util.arguments.ArgumentSpec;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

/**
 * Python docs: <a href="https://docs.python.org/3/library/datetime.html#datetime.date">date objects</a>
 */
public class PythonDate<T extends PythonDate<?>> extends AbstractPythonLikeObject implements PythonLikeComparable<T>,
        PlanningImmutable {
    static final long EPOCH_ORDINAL_OFFSET = Duration.between(LocalDateTime.of(LocalDate.of(0, 12, 31), LocalTime.MIDNIGHT),
            LocalDateTime.of(LocalDate.ofEpochDay(0), LocalTime.MIDNIGHT)).toDays();

    // Ex: Wed Jun  9 04:26:40 1993
    static final DateTimeFormatter C_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM ppd HH:mm:ss yyyy");
    public static PythonLikeType DATE_TYPE = new PythonLikeType("date",
            PythonDate.class);

    public static PythonLikeType $TYPE = DATE_TYPE;

    static {
        try {
            PythonLikeComparable.setup(DATE_TYPE);
            registerMethods();

            DATE_TYPE.$setAttribute("min", new PythonDate(LocalDate.of(1, 1, 1)));
            DATE_TYPE.$setAttribute("max", new PythonDate(LocalDate.of(9999, 12, 31)));
            DATE_TYPE.$setAttribute("resolution", new PythonTimeDelta(Duration.ofDays(1)));

            PythonOverloadImplementor.createDispatchesFor(DATE_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        DATE_TYPE.addConstructor(ArgumentSpec.forFunctionReturning("date", PythonDate.class.getName())
                .addArgument("year", PythonInteger.class.getName())
                .addArgument("month", PythonInteger.class.getName())
                .addArgument("day", PythonInteger.class.getName())
                .asStaticPythonFunctionSignature(PythonDate.class.getMethod("of", PythonInteger.class,
                        PythonInteger.class, PythonInteger.class)));
        // Unary Operators
        DATE_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                PythonDate.class.getMethod("toPythonString"));

        // Binary Operators
        DATE_TYPE.addBinaryMethod(PythonBinaryOperator.ADD,
                PythonDate.class.getMethod("add_time_delta", PythonTimeDelta.class));
        DATE_TYPE.addBinaryMethod(PythonBinaryOperator.SUBTRACT,
                PythonDate.class.getMethod("subtract_time_delta", PythonTimeDelta.class));
        DATE_TYPE.addBinaryMethod(PythonBinaryOperator.SUBTRACT,
                PythonDate.class.getMethod("subtract_date", PythonDate.class));

        // Methods
        DATE_TYPE.addMethod("replace",
                ArgumentSpec.forFunctionReturning("replace", PythonDate.class.getName())
                        .addNullableArgument("year", PythonInteger.class.getName())
                        .addNullableArgument("month", PythonInteger.class.getName())
                        .addNullableArgument("day", PythonInteger.class.getName())
                        .asPythonFunctionSignature(PythonDate.class.getMethod("replace", PythonInteger.class,
                                PythonInteger.class, PythonInteger.class)));
        DATE_TYPE.addMethod("timetuple",
                PythonDate.class.getMethod("timetuple")); // TODO: use time.struct_time type

        DATE_TYPE.addMethod("toordinal",
                PythonDate.class.getMethod("to_ordinal"));
        DATE_TYPE.addMethod("weekday",
                PythonDate.class.getMethod("weekday"));

        DATE_TYPE.addMethod("isoweekday",
                PythonDate.class.getMethod("iso_weekday"));

        DATE_TYPE.addMethod("isoweekday",
                PythonDate.class.getMethod("iso_weekday"));

        DATE_TYPE.addMethod("isocalendar",
                PythonDate.class.getMethod("iso_calendar"));

        DATE_TYPE.addMethod("isoformat",
                PythonDate.class.getMethod("iso_format"));

        DATE_TYPE.addMethod("strftime",
                ArgumentSpec.forFunctionReturning("strftime", PythonString.class.getName())
                        .addArgument("format", PythonString.class.getName())
                        .asPythonFunctionSignature(PythonDate.class.getMethod("strftime", PythonString.class)));

        DATE_TYPE.addMethod("ctime",
                PythonDate.class.getMethod("ctime"));

        // Class methods
        DATE_TYPE.addMethod("today",
                ArgumentSpec.forFunctionReturning("today", PythonDate.class.getName())
                        .addArgument("date_type", PythonLikeType.class.getName())
                        .asClassPythonFunctionSignature(PythonDate.class.getMethod("today",
                                PythonLikeType.class)));

        DATE_TYPE.addMethod("fromtimestamp",
                ArgumentSpec.forFunctionReturning("fromtimestamp", PythonDate.class.getName())
                        .addArgument("date_type", PythonLikeType.class.getName())
                        .addArgument("timestamp", PythonNumber.class.getName())
                        .asClassPythonFunctionSignature(PythonDate.class.getMethod("from_timestamp",
                                PythonLikeType.class,
                                PythonNumber.class)));

        DATE_TYPE.addMethod("fromordinal",
                ArgumentSpec.forFunctionReturning("fromordinal", PythonDate.class.getName())
                        .addArgument("date_type", PythonLikeType.class.getName())
                        .addArgument("ordinal", PythonInteger.class.getName())
                        .asClassPythonFunctionSignature(PythonDate.class.getMethod("from_ordinal",
                                PythonLikeType.class, PythonInteger.class)));

        DATE_TYPE.addMethod("fromisoformat",
                ArgumentSpec.forFunctionReturning("fromisoformat", PythonDate.class.getName())
                        .addArgument("date_type", PythonLikeType.class.getName())
                        .addArgument("date_string", PythonString.class.getName())
                        .asClassPythonFunctionSignature(PythonDate.class.getMethod("from_iso_format",
                                PythonLikeType.class, PythonString.class)));

        DATE_TYPE.addMethod("fromisocalendar",
                ArgumentSpec.forFunctionReturning("fromisocalendar", PythonDate.class.getName())
                        .addArgument("date_type", PythonLikeType.class.getName())
                        .addArgument("year", PythonInteger.class.getName())
                        .addArgument("month", PythonInteger.class.getName())
                        .addArgument("day", PythonInteger.class.getName())
                        .asClassPythonFunctionSignature(PythonDate.class.getMethod("from_iso_calendar", PythonLikeType.class,
                                PythonInteger.class, PythonInteger.class, PythonInteger.class)));
    }

    final LocalDate localDate;

    public final PythonInteger year;
    public final PythonInteger month;
    public final PythonInteger day;

    public PythonDate(LocalDate localDate) {
        this(DATE_TYPE, localDate);
    }

    public PythonDate(PythonLikeType type, LocalDate localDate) {
        super(type);
        this.localDate = localDate;

        this.year = PythonInteger.valueOf(localDate.getYear());
        this.month = PythonInteger.valueOf(localDate.getMonthValue());
        this.day = PythonInteger.valueOf(localDate.getDayOfMonth());
    }

    public static PythonDate of(PythonInteger year, PythonInteger month, PythonInteger day) {
        return of(year.value.intValueExact(), month.value.intValueExact(), day.value.intValueExact());
    }

    public static PythonDate of(int year, int month, int day) {
        if (month < 1 || month > 12) {
            throw new ValueError("month must be between 1 and 12");
        }
        if (!YearMonth.of(year, month).isValidDay(day)) {
            throw new ValueError("day must be between 1 and " + YearMonth.of(year, month).lengthOfMonth());
        }
        return new PythonDate(LocalDate.of(year, month, day));
    }

    @Override
    public PythonLikeObject $getAttributeOrNull(String name) {
        switch (name) {
            case "year":
                return year;
            case "month":
                return month;
            case "day":
                return day;
            default:
                return super.$getAttributeOrNull(name);
        }
    }

    public static PythonDate today() {
        return new PythonDate(LocalDate.now());
    }

    public static PythonDate today(PythonLikeType dateType) {
        if (dateType == DATE_TYPE) {
            return today();
        } else if (dateType == DATE_TIME_TYPE) {
            return today();
        } else {
            throw new TypeError("Unknown date type: " + dateType);
        }
    }

    public static PythonDate from_timestamp(PythonLikeType dateType, PythonNumber timestamp) {
        if (dateType == DATE_TYPE) {
            if (timestamp instanceof PythonInteger) {
                return from_timestamp((PythonInteger) timestamp);
            } else {
                return from_timestamp((PythonFloat) timestamp);
            }
        } else if (dateType == DATE_TIME_TYPE) {
            return PythonDateTime.from_timestamp(dateType, timestamp, PythonNone.INSTANCE);
        } else {
            throw new TypeError("Unknown date type: " + dateType);
        }
    }

    // Python timestamp is in the current System timezone
    public static PythonDate from_timestamp(PythonInteger timestamp) {
        return new PythonDate(LocalDate.ofInstant(Instant.ofEpochSecond(timestamp.getValue().longValue()),
                ZoneId.systemDefault()));
    }

    public static PythonDate from_timestamp(PythonFloat timestamp) {
        return new PythonDate(LocalDate.ofInstant(Instant.ofEpochMilli(
                Math.round(timestamp.getValue().doubleValue() * 1000)),
                ZoneId.systemDefault()));
    }

    public static PythonDate from_ordinal(PythonLikeType dateType, PythonInteger ordinal) {
        if (dateType == DATE_TYPE) {
            return from_ordinal(ordinal);
        } else if (dateType == DATE_TIME_TYPE) {
            return PythonDateTime.from_ordinal(ordinal);
        } else {
            throw new TypeError("Unknown date type: " + dateType);
        }
    }

    public static PythonDate from_ordinal(PythonInteger ordinal) {
        return new PythonDate(LocalDate.ofEpochDay(ordinal.getValue().longValue() - EPOCH_ORDINAL_OFFSET));
    }

    public static PythonDate from_iso_format(PythonLikeType dateType, PythonString dateString) {
        if (dateType == DATE_TYPE) {
            return from_iso_format(dateString);
        } else if (dateType == DATE_TIME_TYPE) {
            return PythonDateTime.from_iso_format(dateString);
        } else {
            throw new TypeError("Unknown date type: " + dateType);
        }
    }

    public static PythonDate from_iso_format(PythonString dateString) {
        return new PythonDate(LocalDate.parse(dateString.getValue()));
    }

    public static PythonDate from_iso_calendar(PythonLikeType dateType, PythonInteger year, PythonInteger week,
            PythonInteger day) {
        if (dateType == DATE_TYPE) {
            return from_iso_calendar(year, week, day);
        } else if (dateType == DATE_TIME_TYPE) {
            return PythonDateTime.from_iso_calendar(year, week, day);
        } else {
            throw new TypeError("Unknown date type: " + dateType);
        }
    }

    public static PythonDate from_iso_calendar(PythonInteger year, PythonInteger week, PythonInteger day) {
        int isoYear = year.getValue().intValue();
        int dayInIsoYear = (week.getValue().intValue() * 7) + day.getValue().intValue();
        int correction = LocalDate.of(isoYear, 1, 4).getDayOfWeek().getValue() + 3;
        int ordinalDate = dayInIsoYear - correction;
        if (ordinalDate <= 0) {
            int daysInYear = LocalDate.ofYearDay(isoYear - 1, 1).lengthOfYear();
            return new PythonDate(LocalDate.ofYearDay(isoYear - 1, ordinalDate + daysInYear));
        } else if (ordinalDate > LocalDate.ofYearDay(isoYear, 1).lengthOfYear()) {
            int daysInYear = LocalDate.ofYearDay(isoYear, 1).lengthOfYear();
            return new PythonDate(LocalDate.ofYearDay(isoYear + 1, ordinalDate - daysInYear));
        } else {
            return new PythonDate(LocalDate.ofYearDay(isoYear, ordinalDate));
        }
    }

    public PythonDate add_time_delta(PythonTimeDelta summand) {
        return new PythonDate(localDate.plusDays(summand.duration.toDays()));
    }

    public PythonDate subtract_time_delta(PythonTimeDelta subtrahend) {
        return new PythonDate(localDate.minusDays(subtrahend.duration.toDays()));
    }

    public PythonTimeDelta subtract_date(PythonDate subtrahend) {
        return new PythonTimeDelta(Duration.ofDays(localDate.toEpochDay() - subtrahend.localDate.toEpochDay()));
    }

    public PythonDate replace(PythonInteger year, PythonInteger month, PythonInteger day) {
        if (year == null) {
            year = this.year;
        }

        if (month == null) {
            month = this.month;
        }

        if (day == null) {
            day = this.day;
        }

        return new PythonDate(LocalDate.of(year.getValue().intValue(),
                month.getValue().intValue(),
                day.getValue().intValue()));
    }

    public PythonLikeTuple timetuple() {
        PythonInteger yday =
                to_ordinal().subtract(PythonDate.of(year.value.intValueExact(), 1, 1).to_ordinal()).add(PythonInteger.ONE);
        return PythonLikeTuple.fromItems(year, month, day,
                PythonInteger.ZERO, PythonInteger.ZERO, PythonInteger.ZERO,
                weekday(), yday, PythonInteger.valueOf(-1));
    }

    public PythonInteger to_ordinal() {
        return PythonInteger.valueOf(localDate.toEpochDay() + EPOCH_ORDINAL_OFFSET);
    }

    public PythonInteger weekday() {
        return PythonInteger.valueOf(localDate.getDayOfWeek().getValue() - 1);
    }

    public PythonInteger iso_weekday() {
        return PythonInteger.valueOf(localDate.getDayOfWeek().getValue());
    }

    public PythonLikeTuple iso_calendar() {
        PythonInteger year = PythonInteger.valueOf(IsoFields.WEEK_BASED_YEAR.getFrom(localDate));
        PythonInteger week = PythonInteger.valueOf(IsoFields.WEEK_OF_WEEK_BASED_YEAR.getFrom(localDate));
        PythonInteger day = PythonInteger.valueOf(localDate.getDayOfWeek().getValue());

        return PythonLikeTuple.fromItems(year, week, day);
    }

    public PythonString iso_format() {
        return new PythonString(localDate.toString());
    }

    public PythonString toPythonString() {
        return new PythonString(toString());
    }

    public PythonString ctime() {
        return new PythonString(localDate.atStartOfDay().format(C_TIME_FORMATTER).replaceAll("(\\D)\\.", "$1"));
    }

    public PythonString strftime(PythonString format) {
        var formatter = PythonDateTimeFormatter.getDateTimeFormatter(format.value);
        return PythonString.valueOf(formatter.format(localDate));
    }

    @Override
    public int compareTo(T date) {
        return localDate.compareTo(date.localDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonDate<?> that = (PythonDate<?>) o;
        return localDate.equals(that.localDate);
    }

    @Override
    public String toString() {
        return iso_format().value;
    }

    @Override
    public int hashCode() {
        return localDate.hashCode();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public PythonString $method$__str__() {
        return iso_format();
    }
}
